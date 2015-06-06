package nodomain.freeyourgadget.gadgetbridge.btle;

import android.bluetooth.BluetoothGatt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBDevice;

/**
 * A special action that is executed at the very front of the initialization
 * sequence (transaction). It will abort the entire initialization sequence
 * by returning false, when the device is already initialized.
 */
public class CheckInitializedAction extends PlainAction {
    private static final Logger LOG = LoggerFactory.getLogger(CheckInitializedAction.class);

    private final GBDevice device;

    public CheckInitializedAction(GBDevice gbDevice) {
        device = gbDevice;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        boolean continueWithOtherInitActions = !device.isInitialized();
        if (!continueWithOtherInitActions) {
            LOG.info("Aborting device initialization, because already initialized: " + device);
        }
        return continueWithOtherInitActions;
    }
}
