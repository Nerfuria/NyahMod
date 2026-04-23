package org.nia.niamod.models.eco;

public sealed interface GameChange permits StatChange, HeadquartersChange, TaxChange,
        BordersChange, RouteChange, LoadoutChange, GlobalTaxChange,
        GlobalBordersChange, GlobalRouteChange {
}
