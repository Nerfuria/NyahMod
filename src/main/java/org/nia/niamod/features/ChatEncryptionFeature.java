package org.nia.niamod.features;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.models.events.ChatEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ChatEncryptionFeature extends Feature {

    private static final int AAD_VALUE = 67;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final String KEY_ALGO = "AES";
    private static final String HASH_ALGO = "SHA-256";
    private static final String MSG_START = "£67-";
    private static final String MSG_END = "-67$";

    private static String encode(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length / 2 + bytes.length % 2);

        for (int i = 0; i < bytes.length - 1; i += 2) {
            int b1 = bytes[i] & 0xFF;
            int b2 = bytes[i + 1] & 0xFF;
            int codePoint = (b1 == 255 && b2 >= 254)
                    ? 1048576 + (b2 - 254)
                    : 983040 + (b1 << 8 | b2);
            builder.appendCodePoint(codePoint);
        }

        if (bytes.length % 2 == 1) {
            builder.appendCodePoint(1048576 + ((bytes[bytes.length - 1] & 0xFF) << 8) + 238);
        }

        return builder.toString();
    }

    private static byte[] decode(String string) {
        List<Byte> byteList = new ArrayList<>();

        for (int cp : string.codePoints().toArray()) {
            if (cp >= 1048576) {
                if ((cp & 255) == 238) {
                    byteList.add((byte) ((cp - 1048576) >> 8));
                } else {
                    byteList.add((byte) 255);
                    byteList.add((byte) ((cp - 1048576) + 2));
                }
            } else {
                int val = cp - 983040;
                byteList.add((byte) (val >> 8));
                byteList.add((byte) (val & 0xFF));
            }
        }

        byte[] result = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) result[i] = byteList.get(i);
        return result;
    }

    @Override
    @Safe
    public void init() {
        ClientSendMessageEvents.MODIFY_CHAT.register(this::processMessage);
        ClientSendMessageEvents.MODIFY_COMMAND.register(this::processMessage);
        ChatEvent.MODIFY.register(this::modifyChat);
    }

    private byte[] encryptionKey() {
        if (NyahConfig.nyahConfigData.encryptionKey.isEmpty()) return new byte[0];
        try {
            return MessageDigest.getInstance(HASH_ALGO)
                    .digest(NyahConfig.nyahConfigData.encryptionKey.getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private GCMParameterSpec gcmSpec(byte[] iv) {
        int saltLength = NyahConfig.nyahConfigData.saltLength;
        byte[] effectiveIv = saltLength == 16 ? iv : Arrays.copyOf(iv, 16);
        return new GCMParameterSpec(GCM_TAG_LENGTH, effectiveIv);
    }

    private Cipher initCipher(int mode, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(mode, new SecretKeySpec(encryptionKey(), KEY_ALGO), gcmSpec(iv));
            cipher.updateAAD(ByteBuffer.allocate(4).putInt(AAD_VALUE).array());
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String encryptMessage(String message) {
        try {
            byte[] iv = new byte[NyahConfig.nyahConfigData.saltLength];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, iv);
            byte[] encrypted = cipher.doFinal(message.trim().getBytes());
            return MSG_START + encode(iv) + encode(encrypted) + MSG_END;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(String message) throws AEADBadTagException {
        try {
            byte[] bytes = decode(message);
            int saltLength = NyahConfig.nyahConfigData.saltLength;
            byte[] iv = Arrays.copyOfRange(bytes, 0, saltLength);
            Cipher cipher = initCipher(Cipher.DECRYPT_MODE, iv);
            byte[] decrypted = cipher.doFinal(bytes, saltLength, bytes.length - saltLength);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (AEADBadTagException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Safe(ordinal = 0)
    public String processMessage(String message) {
        if (!NyahConfig.nyahConfigData.encryptionEnabled) return message;
        String prefix = NyahConfig.nyahConfigData.encryptionPrefix;
        int idx = message.indexOf(prefix);
        if (idx == -1) return message;
        return message.substring(0, idx) + encryptMessage(message.substring(idx + prefix.length()));
    }

    public String decodeMessage(String message) {
        int start = message.indexOf(MSG_START);
        int end = message.indexOf(MSG_END);
        if (start == -1 || end == -1) return message;
        try {
            return decrypt(message.substring(start + MSG_START.length(), end));
        } catch (AEADBadTagException e) {
            return message;
        }
    }

    @Safe(ordinal = 0)
    public Component modifyChat(Component text) {
        if (!NyahConfig.nyahConfigData.encryptionEnabled) return text;
        MutableComponent copy = Component.empty();
        text.visit((style, string) -> {
            String decoded = decodeMessage(string);
            if (!decoded.equals(string)) {
                copy.append(Component.literal(string.substring(0, string.indexOf(MSG_START))).withStyle(style));
                style = style
                        .withHoverEvent(new HoverEvent.ShowText(Component.nullToEmpty("This message was encoded with nyah-s :3")))
                        .withUnderlined(true);
            }
            copy.append(Component.literal(decoded).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);

        return copy;
    }
}