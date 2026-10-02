package com.eveningoutpost.dexdrip.watch.thinjam;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

public class BlueJayAdapter {

    public static int screenTimeoutValueToSeconds(final int value) {
        switch (value) {
            case 0:
                return 2;
            case 1:
                return 4;
            case 2:
                return 6;
            case 3:
                return 8;
            case 4:
                return 10;
            case 5:
                return 12;
            case 6:
                return 16;
            case 7:
                return 20;
            default:
                return -1; // invalid

        }
    }

    /**
     * Guard for `bluejay_run_as_phone_collector` (BlueJay occupies the phone slot). Shared by the
     * Compose settings screen.
     */
    public static boolean canUsePhoneSlot(final boolean newValue) {
        if (!newValue) {
            return true;
        }
        try {
            if (Pref.getBoolean("bluejay_run_phone_collector", true)) {
                JoH.static_toast_long("Must disable phone collector first!");
                return false;
            }
            final String mac = BlueJay.getMac();
            if (mac == null) {
                JoH.static_toast_long("Needs a connected BlueJay");
                return false;
            }
            if (BlueJayInfo.getInfo(mac).buildNumber < 51) {
                JoH.static_toast_long("Needs BlueJay firmware at least version 51");
                return false;
            }
        } catch (Exception e) {
            //
        }
        return true;
    }

    /**
     * Guard for `bluejay_run_phone_collector` (phone runs the standard collector). Shared by the
     * Compose settings screen.
     */
    public static boolean canRunPhoneCollector(final boolean newValue) {
        if (!newValue) {
            return true;
        }
        try {
            if (!alwaysAllowPhoneSlot() && Pref.getBoolean("bluejay_run_as_phone_collector", false)) {
                JoH.static_toast_long("Must disable BlueJay using phone slot first!");
                return false;
            }
        } catch (Exception e) {
            //
        }
        return true;
    }

    private static boolean alwaysAllowPhoneSlot() {
        final int specifiedSlot = Pref.getBooleanDefaultFalse("engineering_mode") ? Pref.getStringToInt("dex_specified_slot", -1) : -1;
        return specifiedSlot == 3;
    }

}
