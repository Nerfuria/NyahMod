package org.nia.niamod.features;

import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SystemMessageEvent;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.nia.niamod.config.NyahConfig;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ChatEncryptionFeature {

    public void init() {
        ClientSendMessageEvents.MODIFY_CHAT.register(this::processMessage);
        ClientSendMessageEvents.MODIFY_COMMAND.register(this::processMessage);
    }

    private static String encode(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length / 2 + bytes.length % 2);

        for (int i = 0; i < bytes.length - 1; i += 2) {
            int b1 = bytes[i] & 0xFF;
            int b2 = bytes[i + 1] & 0xFF;

            int codePoint;
            if (b1 == 255 && b2 >= 254) {
                codePoint = 1048576 + (b2 - 254);
            } else {
                codePoint = 983040 + (b1 << 8 | b2);
            }

            builder.appendCodePoint(codePoint);
        }

        if (bytes.length % 2 == 1) {
            int last = bytes[bytes.length - 1] & 0xFF;
            builder.appendCodePoint(1048576 + (last << 8) + 238);
        }

        return builder.toString();
    }

    private static byte[] decode(String string) {
        int[] codePoints = string.codePoints().toArray();
        List<Byte> byteList = new ArrayList<>();

        for (int cp : codePoints) {
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
        for (int i = 0; i < byteList.size(); i++) {
            result[i] = byteList.get(i);
        }
        return result;
    }

    private String encryptMessage(String message) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }

        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey(), "AES");
        byte[] iv = new byte[NyahConfig.nyahConfigData.saltLength];
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, NyahConfig.nyahConfigData.saltLength == 16 ? new GCMParameterSpec(128, iv) : new GCMParameterSpec(128, Arrays.copyOf(iv, 16)));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        cipher.updateAAD(ByteBuffer.allocate(4).putInt(67).array());
        try {
            return String.format("£67-%s%s-67$", encode(iv), encode(cipher.doFinal(message.trim().getBytes())));
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private String decrypt(String message) throws AEADBadTagException {
        byte[] bytes = decode(message);

        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey(), "AES");
        byte[] iv = Arrays.copyOfRange(bytes, 0, NyahConfig.nyahConfigData.saltLength);

        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, NyahConfig.nyahConfigData.saltLength == 16 ? new GCMParameterSpec(128, iv) : new GCMParameterSpec(128, Arrays.copyOf(iv, 16)));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
        cipher.updateAAD(ByteBuffer.allocate(4).putInt(67).array());
        byte[] decrypted =
                null;
        try {
            decrypted = cipher.doFinal(bytes, NyahConfig.nyahConfigData.saltLength, bytes.length - NyahConfig.nyahConfigData.saltLength);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private byte[] encryptionKey() {
        if (NyahConfig.nyahConfigData.encryptionKey.isEmpty()) return new byte[0];
        try {
            return MessageDigest.getInstance("SHA-256").digest(NyahConfig.nyahConfigData.encryptionKey.getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String processMessage(String message) {
        if (!message.contains(NyahConfig.nyahConfigData.encryptionPrefix)) return message;

        return message.substring(0, message.indexOf(NyahConfig.nyahConfigData.encryptionPrefix)) + encryptMessage(message.substring(message.indexOf(NyahConfig.nyahConfigData.encryptionPrefix) + 1));
    }

    public String decodeMessage(String message) {
        int start = message.indexOf("£67-");
        int end = message.indexOf("-67$");
        if (start == -1 || end == -1) return message;
        try {
            return decrypt(message.substring(start + 4, end));
        } catch (AEADBadTagException e) {
            return message;
        }
    }

    public Text modifyChat(Text text, boolean overlay) {
        if (overlay) return text;
        MutableText copy = Text.empty();
        text.visit((style, string) -> {
            String decoded = decodeMessage(string);
            if (!decoded.equals(string)) {
                String s = string.substring(0, string.indexOf("£67-"));
                copy.append(Text.literal(s).fillStyle(style));
                style = style.withHoverEvent(new HoverEvent.ShowText(Text.of("This message was encoded with nyah-s :3"))).withUnderline(true);
            }
            copy.append(Text.literal(decoded).fillStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return copy;
    }

}
