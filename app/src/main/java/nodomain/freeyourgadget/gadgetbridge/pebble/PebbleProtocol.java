package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBCommand;
import nodomain.freeyourgadget.gadgetbridge.GBDeviceApp;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommand;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandAppManagementResult;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandCallControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandMusicControl;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandSendBytes;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceCommandVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;

public class PebbleProtocol extends GBDeviceProtocol {

    private static final Logger LOG = LoggerFactory.getLogger(PebbleProtocol.class);

    // set to false AT YOUR OWN RISK. I ended up in recovery
    static private final boolean USE_OLD_NOTIFICATION_PROTOCOL = true;

    static final short ENDPOINT_FIRMWARE = 1;
    static final short ENDPOINT_TIME = 11;
    static final short ENDPOINT_FIRMWAREVERSION = 16;
    public static final short ENDPOINT_PHONEVERSION = 17;
    static final short ENDPOINT_SYSTEMMESSAGE = 18;
    static final short ENDPOINT_MUSICCONTROL = 32;
    static final short ENDPOINT_PHONECONTROL = 33;
    static final short ENDPOINT_APPLICATIONMESSAGE = 48;
    static final short ENDPOINT_LAUNCHER = 49;
    static final short ENDPOINT_LOGS = 2000;
    static final short ENDPOINT_PING = 2001;
    static final short ENDPOINT_LOGDUMP = 2002;
    static final short ENDPOINT_RESET = 2003;
    static final short ENDPOINT_APP = 2004;
    static final short ENDPOINT_APPLOGS = 2006;
    static final short ENDPOINT_NOTIFICATION = 3000;
    static final short ENDPOINT_EXTENSIBLENOTIFS = 3010;
    static final short ENDPOINT_RESOURCE = 4000;
    static final short ENDPOINT_SYSREG = 5000;
    static final short ENDPOINT_FCTREG = 5001;
    static final short ENDPOINT_APPMANAGER = 6000;
    public static final short ENDPOINT_DATALOG = 6778;
    static final short ENDPOINT_RUNKEEPER = 7000;
    static final short ENDPOINT_SCREENSHOT = 8000;
    static final short ENDPOINT_PUTBYTES = (short) 48879;

    static final byte NOTIFICATION_EMAIL = 0;
    static final byte NOTIFICATION_SMS = 1;
    static final byte NOTIFICATION_TWITTER = 2;
    static final byte NOTIFICATION_FACEBOOK = 3;

    static final byte PHONECONTROL_ANSWER = 1;
    static final byte PHONECONTROL_HANGUP = 2;
    static final byte PHONECONTROL_GETSTATE = 3;
    static final byte PHONECONTROL_INCOMINGCALL = 4;
    static final byte PHONECONTROL_OUTGOINGCALL = 5;
    static final byte PHONECONTROL_MISSEDCALL = 6;
    static final byte PHONECONTROL_RING = 7;
    static final byte PHONECONTROL_START = 8;
    static final byte PHONECONTROL_END = 9;

    static final byte MUSICCONTROL_SETMUSICINFO = 16;
    static final byte MUSICCONTROL_PLAYPAUSE = 1;
    static final byte MUSICCONTROL_PAUSE = 2;
    static final byte MUSICCONTROL_PLAY = 3;
    static final byte MUSICCONTROL_NEXT = 4;
    static final byte MUSICCONTROL_PREVIOUS = 5;
    static final byte MUSICCONTROL_VOLUMEUP = 6;
    static final byte MUSICCONTROL_VOLUMEDOWN = 7;
    static final byte MUSICCONTROL_GETNOWPLAYING = 7;

    static final byte TIME_GETTIME = 0;
    static final byte TIME_SETTIME = 2;

    static final byte FIRMWAREVERSION_GETVERSION = 0;

    static final byte APPMANAGER_GETAPPBANKSTATUS = 1;
    static final byte APPMANAGER_REMOVEAPP = 2;
    static final byte APPMANAGER_REFRESHAPP = 3;
    static final byte APPMANAGER_GETUUIDS = 5;

    static final int APPMANAGER_RES_SUCCESS = 1;

    static final byte APPLICATIONMESSAGE_PUSH = 1;
    static final byte APPLICATIONMESSAGE_REQUEST = 2;
    static final byte APPLICATIONMESSAGE_ACK = (byte) 0xff;
    static final byte APPLICATIONMESSAGE_NACK = (byte) 0x7f;

    static final byte DATALOG_TIMEOUT = 7;

    static final byte PUTBYTES_INIT = 1;
    static final byte PUTBYTES_SEND = 2;
    static final byte PUTBYTES_COMMIT = 3;
    static final byte PUTBYTES_ABORT = 4;
    static final byte PUTBYTES_COMPLETE = 5;

    static final byte PUTBYTES_TYPE_FIRMWARE = 1;
    static final byte PUTBYTES_TYPE_RECOVERY = 2;
    static final byte PUTBYTES_TYPE_SYSRESOURCES = 3;
    public static final byte PUTBYTES_TYPE_RESOURCES = 4;
    public static final byte PUTBYTES_TYPE_BINARY = 5;
    static final byte PUTBYTES_TYPE_FILE = 6;
    public static final byte PUTBYTES_TYPE_WORKER = 7;

    public static final byte RESET_REBOOT = 0;

    private final byte SYSTEMMESSAGE_FIRMWARESTART = 1;
    private final byte SYSTEMMESSAGE_FIRMWARECOMPLETE = 2;
    private final byte SYSTEMMESSAGE_FIRMWAREFAIL = 3;

    static final byte PHONEVERSION_REQUEST = 0;
    static final byte PHONEVERSION_APPVERSION_MAGIC = 2; // increase this if pebble complains
    static final byte PHONEVERSION_APPVERSION_MAJOR = 2;
    static final byte PHONEVERSION_APPVERSION_MINOR = 3;
    static final byte PHONEVERSION_APPVERSION_PATCH = 0;


    static final int PHONEVERSION_SESSION_CAPS_GAMMARAY = 0x80000000;

    static final int PHONEVERSION_REMOTE_CAPS_TELEPHONY = 0x00000010;
    static final int PHONEVERSION_REMOTE_CAPS_SMS = 0x00000020;
    static final int PHONEVERSION_REMOTE_CAPS_GPS = 0x00000040;
    static final int PHONEVERSION_REMOTE_CAPS_BTLE = 0x00000080;
    static final int PHONEVERSION_REMOTE_CAPS_REARCAMERA = 0x00000100;
    static final int PHONEVERSION_REMOTE_CAPS_ACCEL = 0x00000200;
    static final int PHONEVERSION_REMOTE_CAPS_GYRO = 0x00000400;
    static final int PHONEVERSION_REMOTE_CAPS_COMPASS = 0x00000800;

    static final byte PHONEVERSION_REMOTE_OS_UNKNOWN = 0;
    static final byte PHONEVERSION_REMOTE_OS_IOS = 1;
    static final byte PHONEVERSION_REMOTE_OS_ANDROID = 2;
    static final byte PHONEVERSION_REMOTE_OS_OSX = 3;
    static final byte PHONEVERSION_REMOTE_OS_LINUX = 4;
    static final byte PHONEVERSION_REMOTE_OS_WINDOWS = 5;

    static final byte TYPE_BYTEARRAY = 0;
    static final byte TYPE_CSTRING = 1;
    static final byte TYPE_UINT32 = 2;
    static final byte TYPE_INT32 = 3;

    static final short LENGTH_PREFIX = 4;
    static final short LENGTH_SETTIME = 5;
    static final short LENGTH_GETTIME = 1;
    static final short LENGTH_REMOVEAPP = 17;
    static final short LENGTH_REFRESHAPP = 5;
    static final short LENGTH_PHONEVERSION = 17;
    static final short LENGTH_UPLOADSTART = 7;
    static final short LENGTH_UPLOADCHUNK = 9;
    static final short LENGTH_UPLOADCOMMIT = 9;
    static final short LENGTH_UPLOADCOMPLETE = 5;
    static final short LENGTH_UPLOADCANCEL = 5;
    static final short LENGTH_SYSTEMMESSAGE = 2;

    private static final String[] hwRevisions = {"unknown", "ev1", "ev2", "ev2_3", "ev2_4", "v1_5", "v2_0"};
    private static Random mRandom = new Random();


    byte last_id = -1;
    private ArrayList<UUID> tmpUUIDS = new ArrayList<>();

    private MorpheuzSupport mMorpheuzSupport = new MorpheuzSupport(PebbleProtocol.this);
    // FIXME: this does not belong here
    static final UUID WeatherNeatUUID = UUID.fromString("3684003b-a685-45f9-a713-abc6364ba051");


    private static byte[] encodeMessage(short endpoint, byte type, int cookie, String[] parts) {
        // Calculate length first
        int length = LENGTH_PREFIX + 1;
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    length++; // encode null or empty strings as 0x00 later
                    continue;
                }
                length += (1 + s.getBytes().length);
            }
        }
        if (endpoint == ENDPOINT_PHONECONTROL) {
            length += 4; //for cookie;
        }

        // Encode Prefix
        ByteBuffer buf = ByteBuffer.allocate(length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (length - LENGTH_PREFIX));
        buf.putShort(endpoint);
        buf.put(type);

        if (endpoint == ENDPOINT_PHONECONTROL) {
            buf.putInt(cookie);
        }
        // Encode Pascal-Style Strings
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    //buf.put((byte)0x01);
                    buf.put((byte) 0x00);
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 255) partlength = 255;
                buf.put((byte) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }
        return buf.array();
    }

    @Override
    public byte[] encodeSMS(String from, String body) {
        Long ts = System.currentTimeMillis() / 1000;
        TimeZone tz = SimpleTimeZone.getDefault();
        ts += (tz.getOffset(ts) + tz.getDSTSavings()) / 1000;

        if (USE_OLD_NOTIFICATION_PROTOCOL) {
            String[] parts = {from, body, ts.toString()};
            return encodeMessage(ENDPOINT_NOTIFICATION, NOTIFICATION_SMS, 0, parts);
        }

        String[] parts = {from, null, body};
        return encodeExtensibleNotification(mRandom.nextInt(), (int) (ts & 0xffffffff), parts);
    }

    @Override
    public byte[] encodeEmail(String from, String subject, String body) {
        Long ts = System.currentTimeMillis() / 1000;
        TimeZone tz = SimpleTimeZone.getDefault();
        ts += (tz.getOffset(ts) + tz.getDSTSavings()) / 1000;
        String tsstring = ts.toString(); // SIC
        String[] parts = {from, body, tsstring, subject};

        return encodeMessage(ENDPOINT_NOTIFICATION, NOTIFICATION_EMAIL, 0, parts);
    }

    @Override
    public byte[] encodeGenericNotification(String title, String details) {
        return encodeSMS(title, details);
    }

    @Override
    public byte[] encodeSetTime(long ts) {
        if (ts == -1) {
            ts = System.currentTimeMillis() / 1000;
            TimeZone tz = SimpleTimeZone.getDefault();
            ts += (tz.getOffset(ts) + tz.getDSTSavings()) / 1000;
        }
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SETTIME);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_SETTIME);
        buf.putShort(ENDPOINT_TIME);
        buf.put(TIME_SETTIME);
        buf.putInt((int) ts);

        return buf.array();
    }

    private static byte[] encodeExtensibleNotification(int id, int timestamp, String[] parts) {
        // Calculate length first
        byte attributes_count = 0;

        int length = 21;
        if (parts != null) {
            for (String s : parts) {
                if (s == null || s.equals("")) {
                    continue;
                }
                attributes_count++;
                length += (3 + s.getBytes().length);
            }
        }

        // Encode Prefix
        ByteBuffer buf = ByteBuffer.allocate(length + LENGTH_PREFIX);

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (length));
        buf.putShort(ENDPOINT_EXTENSIBLENOTIFS);

        buf.order(ByteOrder.LITTLE_ENDIAN); // !

        buf.put((byte) 0x00); // ?
        buf.put((byte) 0x01); // add notifications
        buf.putInt(0x00000002); // flags - ?
        buf.putInt(id);
        buf.putInt(0x00000000); // ANCS id
        buf.putInt(timestamp);
        buf.put((byte) 0x01); // layout - ?
        buf.put(attributes_count); // length attributes
        buf.put((byte) 0); // len actions - none so far

        byte attribute_id = 0;
        // Encode Pascal-Style Strings
        if (parts != null) {
            for (String s : parts) {
                attribute_id++;
                if (s == null || s.equals("")) {
                    continue;
                }

                int partlength = s.getBytes().length;
                if (partlength > 255) partlength = 255;
                buf.put(attribute_id);
                buf.putShort((short) partlength);
                buf.put(s.getBytes(), 0, partlength);
            }
        }
        // ACTION
        /*
        buf.put((byte) 0x01);
        buf.put((byte) 0x02);
        buf.put((byte) 0x01);

        String actionstring = "test";

        buf.put((byte) 0x01);
        buf.putShort((short) 4);
        buf.put(actionstring.getBytes(), 0, 4);
        */
        return buf.array();
    }

    public byte[] encodeGetTime() {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_GETTIME);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_GETTIME);
        buf.putShort(ENDPOINT_TIME);
        buf.put(TIME_GETTIME);

        return buf.array();
    }

    @Override
    public byte[] encodeSetCallState(String number, String name, GBCommand command) {
        String[] parts = {number, name};
        byte pebbleCmd;
        switch (command) {
            case CALL_START:
                pebbleCmd = PHONECONTROL_START;
                break;
            case CALL_END:
                pebbleCmd = PHONECONTROL_END;
                break;
            case CALL_INCOMING:
                pebbleCmd = PHONECONTROL_INCOMINGCALL;
                break;
            case CALL_OUTGOING:
                // pebbleCmd = PHONECONTROL_OUTGOINGCALL;
                /*
                 *  HACK/WORKAROUND for non-working outgoing call display.
                 *  Just send a incoming call command immediately followed by a start call command
                 *  This prevents vibration of the Pebble.
                 */
                byte[] callmsg = encodeMessage(ENDPOINT_PHONECONTROL, PHONECONTROL_INCOMINGCALL, 0, parts);
                byte[] startmsg = encodeMessage(ENDPOINT_PHONECONTROL, PHONECONTROL_START, 0, parts);
                byte[] msg = new byte[callmsg.length + startmsg.length];
                System.arraycopy(callmsg, 0, msg, 0, callmsg.length);
                System.arraycopy(startmsg, 0, msg, startmsg.length, startmsg.length);
                return msg;
            // END HACK
            default:
                return null;
        }
        return encodeMessage(ENDPOINT_PHONECONTROL, pebbleCmd, 0, parts);
    }

    @Override
    public byte[] encodeSetMusicInfo(String artist, String album, String track) {
        String[] parts = {artist, album, track};
        return encodeMessage(ENDPOINT_MUSICCONTROL, MUSICCONTROL_SETMUSICINFO, 0, parts);
    }

    @Override
    public byte[] encodeFirmwareVersionReq() {
        return encodeMessage(ENDPOINT_FIRMWAREVERSION, FIRMWAREVERSION_GETVERSION, 0, null);
    }

    @Override
    public byte[] encodeAppInfoReq() {
        return encodeMessage(ENDPOINT_APPMANAGER, APPMANAGER_GETUUIDS, 0, null);
    }

    @Override
    public byte[] encodeAppStart(UUID uuid) {
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(1, (Object) 1)); // launch
        return encodeApplicationMessagePush(ENDPOINT_LAUNCHER, uuid, pairs);
    }

    @Override
    public byte[] encodeAppDelete(UUID uuid) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_REMOVEAPP);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_REMOVEAPP);
        buf.putShort(ENDPOINT_APPMANAGER);
        buf.put(APPMANAGER_REMOVEAPP);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());

        return buf.array();
    }

    @Override
    public byte[] encodePhoneVersion(byte os) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_PHONEVERSION);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_PHONEVERSION);
        buf.putShort(ENDPOINT_PHONEVERSION);
        buf.put((byte) 0x01);
        buf.putInt(-1); //0xffffffff

        if (os == PHONEVERSION_REMOTE_OS_ANDROID) {
            buf.putInt(PHONEVERSION_SESSION_CAPS_GAMMARAY);
        } else {
            buf.putInt(0);
        }
        buf.putInt(PHONEVERSION_REMOTE_CAPS_SMS | PHONEVERSION_REMOTE_CAPS_TELEPHONY | os);

        buf.put(PHONEVERSION_APPVERSION_MAGIC);
        buf.put(PHONEVERSION_APPVERSION_MAJOR);
        buf.put(PHONEVERSION_APPVERSION_MINOR);
        buf.put(PHONEVERSION_APPVERSION_PATCH);

        return buf.array();
    }

    @Override
    public byte[] encodeReboot() {
        return encodeMessage(ENDPOINT_RESET, RESET_REBOOT, 0, null);
    }

    /* pebble specific install methods */
    public byte[] encodeUploadStart(byte type, byte index, int size) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADSTART);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADSTART);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_INIT);
        buf.putInt(size);
        buf.put(type);
        buf.put(index);
        return buf.array();
    }

    public byte[] encodeUploadChunk(int token, byte[] buffer, int size) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCHUNK + size);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) (LENGTH_UPLOADCHUNK + size));
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_SEND);
        buf.putInt(token);
        buf.putInt(size);
        buf.put(buffer, 0, size);
        return buf.array();
    }

    public byte[] encodeUploadCommit(int token, int crc) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCOMMIT);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCOMMIT);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_COMMIT);
        buf.putInt(token);
        buf.putInt(crc);
        return buf.array();
    }

    public byte[] encodeUploadComplete(int token) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCOMPLETE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCOMPLETE);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_COMPLETE);
        buf.putInt(token);
        return buf.array();
    }

    public byte[] encodeUploadCancel(int token) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_UPLOADCANCEL);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_UPLOADCANCEL);
        buf.putShort(ENDPOINT_PUTBYTES);
        buf.put(PUTBYTES_ABORT);
        buf.putInt(token);
        return buf.array();
    }

    private byte[] encodeSystemMessage(byte systemMessage) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_SYSTEMMESSAGE);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_SYSTEMMESSAGE);
        buf.putShort(ENDPOINT_SYSTEMMESSAGE);
        buf.put((byte) 0);
        buf.put(systemMessage);
        return buf.array();

    }

    public byte[] encodeInstallFirmwareStart() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWARESTART);
    }

    public byte[] encodeInstallFirmwareComplete() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWARECOMPLETE);
    }

    public byte[] encodeInstallFirmwareError() {
        return encodeSystemMessage(SYSTEMMESSAGE_FIRMWAREFAIL);
    }


    public byte[] encodeAppRefresh(int index) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + LENGTH_REFRESHAPP);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort(LENGTH_REFRESHAPP);
        buf.putShort(ENDPOINT_APPMANAGER);
        buf.put(APPMANAGER_REFRESHAPP);
        buf.putInt(index);

        return buf.array();
    }

    public byte[] encodeDatalog(byte handle, byte reply) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + 2);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 2);
        buf.putShort(ENDPOINT_DATALOG);
        buf.put(reply);
        buf.put(handle);

        return buf.array();
    }

    private byte[] encodeApplicationMessageWeatherNeatTest() {
        // encode push message for WeatherNeat
        ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();
        pairs.add(new Pair<>(1, (Object) "Gadgetbridge")); // city
        pairs.add(new Pair<>(2, (Object) "-22C")); // temperature
        pairs.add(new Pair<>(3, (Object) "this is just a stupid test")); // condition
        pairs.add(new Pair<>(5, (Object) 3)); // seconds for backlight on shake
        byte[] testMessage = encodeApplicationMessagePush(ENDPOINT_APPLICATIONMESSAGE, WeatherNeatUUID, pairs);

        ByteBuffer buf = ByteBuffer.allocate(testMessage.length + LENGTH_PREFIX + 18); // +ACK

        // encode ack and put in front of push message (hack for acknowledging the last message)
        buf.put(encodeApplicationMessageAck(WeatherNeatUUID, (byte) (last_id - 1)));
        buf.put(testMessage);

        return buf.array();
    }

    byte[] encodeApplicationMessageAck(UUID uuid, byte id) {
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + 18); // +ACK

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) 18);
        buf.putShort(ENDPOINT_APPLICATIONMESSAGE);
        buf.put(APPLICATIONMESSAGE_ACK);
        buf.put(id);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getMostSignificantBits());

        return buf.array();
    }


    private ArrayList<Pair<Integer, Object>> decodeDict(ByteBuffer buf) {
        ArrayList<Pair<Integer, Object>> dict = new ArrayList<Pair<Integer, Object>>();
        buf.order(ByteOrder.LITTLE_ENDIAN);
        byte dictSize = buf.get();
        while (dictSize-- > 0) {
            Integer key = buf.getInt();
            byte type = buf.get();
            short length = buf.getShort(); // length
            switch (type) {
                case TYPE_INT32:
                case TYPE_UINT32:
                    dict.add(new Pair<Integer, Object>(key, buf.getInt()));
                    break;
                case TYPE_CSTRING:
                case TYPE_BYTEARRAY:
                    byte[] bytes = new byte[length];
                    buf.get(bytes);
                    if (type == TYPE_BYTEARRAY) {
                        dict.add(new Pair<Integer, Object>(key, bytes));
                    } else {
                        dict.add(new Pair<Integer, Object>(key, Arrays.toString(bytes)));
                    }
                    break;
                default:
            }
        }
        return dict;
    }

    byte[] encodeApplicationMessagePush(short endpoint, UUID uuid, ArrayList<Pair<Integer, Object>> pairs) {
        int length = 16 + 3; // UUID + (PUSH + id + length of dict)
        for (Pair<Integer, Object> pair : pairs) {
            length += 7; // key + type + length
            if (pair.second instanceof Integer) {
                length += 4;
            } else if (pair.second instanceof String) {
                length += ((String) pair.second).length() + 1;
            }
        }
        ByteBuffer buf = ByteBuffer.allocate(LENGTH_PREFIX + length);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putShort((short) length);
        buf.putShort(endpoint); // 48 or 49
        buf.put(APPLICATIONMESSAGE_PUSH);
        buf.put(++last_id);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        buf.put((byte) pairs.size());

        buf.order(ByteOrder.LITTLE_ENDIAN); // Um, yes, really
        for (Pair<Integer, Object> pair : pairs) {
            buf.putInt(pair.first);
            if (pair.second instanceof Integer) {
                buf.put(TYPE_INT32);
                buf.putShort((short) 4); // length of int
                buf.putInt((int) pair.second);
            } else if (pair.second instanceof String) {
                buf.put(TYPE_CSTRING);
                buf.putShort((short) (((String) pair.second).length() + 1));
                buf.put(((String) pair.second).getBytes());
                buf.put((byte) 0);
            }
        }

        return buf.array();
    }


    @Override
    public GBDeviceCommand decodeResponse(byte[] responseData) {
        ByteBuffer buf = ByteBuffer.wrap(responseData);
        buf.order(ByteOrder.BIG_ENDIAN);
        short length = buf.getShort();
        short endpoint = buf.getShort();
        byte pebbleCmd = buf.get();
        GBDeviceCommand cmd = null;
        switch (endpoint) {
            case ENDPOINT_MUSICCONTROL:
                GBDeviceCommandMusicControl musicCmd = new GBDeviceCommandMusicControl();
                switch (pebbleCmd) {
                    case MUSICCONTROL_NEXT:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.NEXT;
                        break;
                    case MUSICCONTROL_PREVIOUS:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PREVIOUS;
                        break;
                    case MUSICCONTROL_PLAY:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PLAY;
                        break;
                    case MUSICCONTROL_PAUSE:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PAUSE;
                        break;
                    case MUSICCONTROL_PLAYPAUSE:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.PLAYPAUSE;
                        break;
                    case MUSICCONTROL_VOLUMEUP:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.VOLUMEUP;
                        break;
                    case MUSICCONTROL_VOLUMEDOWN:
                        musicCmd.command = GBDeviceCommandMusicControl.Command.VOLUMEDOWN;
                        break;
                    default:
                        break;
                }
                cmd = musicCmd;
                break;
            case ENDPOINT_PHONECONTROL:
                GBDeviceCommandCallControl callCmd = new GBDeviceCommandCallControl();
                switch (pebbleCmd) {
                    case PHONECONTROL_HANGUP:
                        callCmd.command = GBDeviceCommandCallControl.Command.END;
                        break;
                    default:
                        LOG.info("Unknown PHONECONTROL command" + pebbleCmd);
                        break;
                }
                cmd = callCmd;
                break;
            case ENDPOINT_FIRMWAREVERSION:
                GBDeviceCommandVersionInfo versionCmd = new GBDeviceCommandVersionInfo();

                buf.getInt(); // skip
                byte[] tmp = new byte[32];
                buf.get(tmp, 0, 32);

                versionCmd.fwVersion = new String(tmp).trim();
                buf.get(tmp, 0, 9);
                Byte hwRev = buf.get();
                if (hwRev > 0 && hwRev < hwRevisions.length) {
                    versionCmd.hwVersion = hwRevisions[hwRev];
                }
                cmd = versionCmd;
                break;
            case ENDPOINT_APPMANAGER:
                switch (pebbleCmd) {
                    case APPMANAGER_GETAPPBANKSTATUS:
                        GBDeviceCommandAppInfo appInfoCmd = new GBDeviceCommandAppInfo();
                        int slotCount = buf.getInt();
                        int slotsUsed = buf.getInt();
                        byte[] appName = new byte[32];
                        byte[] appCreator = new byte[32];
                        appInfoCmd.apps = new GBDeviceApp[slotsUsed];
                        boolean[] slotInUse = new boolean[slotCount];

                        for (int i = 0; i < slotsUsed; i++) {
                            int id = buf.getInt();
                            int index = buf.getInt();
                            slotInUse[index] = true;
                            buf.get(appName, 0, 32);
                            buf.get(appCreator, 0, 32);
                            int flags = buf.getInt();

                            GBDeviceApp.Type appType;
                            if ((flags & 16) == 16) {  // FIXME: verify this assumption
                                appType = GBDeviceApp.Type.APP_ACTIVITYTRACKER;
                            } else if ((flags & 1) == 1) {  // FIXME: verify this assumption
                                appType = GBDeviceApp.Type.WATCHFACE;
                            } else {
                                appType = GBDeviceApp.Type.APP_GENERIC;
                            }
                            Short appVersion = buf.getShort();
                            appInfoCmd.apps[i] = new GBDeviceApp(tmpUUIDS.get(i), new String(appName).trim(), new String(appCreator).trim(), appVersion.toString(), appType);
                        }
                        for (int i = 0; i < slotCount; i++) {
                            if (!slotInUse[i]) {
                                appInfoCmd.freeSlot = (byte) i;
                                LOG.info("found free slot " + i);
                                break;
                            }
                        }
                        cmd = appInfoCmd;
                        break;
                    case APPMANAGER_GETUUIDS:
                        GBDeviceCommandSendBytes sendBytes = new GBDeviceCommandSendBytes();
                        sendBytes.encodedBytes = encodeMessage(ENDPOINT_APPMANAGER, APPMANAGER_GETAPPBANKSTATUS, 0, null);
                        cmd = sendBytes;
                        tmpUUIDS.clear();
                        slotsUsed = buf.getInt();
                        for (int i = 0; i < slotsUsed; i++) {
                            long uuid_high = buf.getLong();
                            long uuid_low = buf.getLong();
                            UUID uuid = new UUID(uuid_high, uuid_low);
                            LOG.info("found uuid: " + uuid);
                            tmpUUIDS.add(uuid);
                        }
                        break;
                    case APPMANAGER_REMOVEAPP:
                        GBDeviceCommandAppManagementResult deleteRes = new GBDeviceCommandAppManagementResult();
                        deleteRes.type = GBDeviceCommandAppManagementResult.CommandType.DELETE;

                        int result = buf.getInt();
                        switch (result) {
                            case APPMANAGER_RES_SUCCESS:
                                deleteRes.result = GBDeviceCommandAppManagementResult.Result.SUCCESS;
                                break;
                            default:
                                deleteRes.result = GBDeviceCommandAppManagementResult.Result.FAILURE;
                                break;
                        }
                        cmd = deleteRes;
                        break;
                    default:
                        LOG.info("Unknown APPMANAGER command" + pebbleCmd);
                        break;
                }
                break;
            case ENDPOINT_PUTBYTES:
                GBDeviceCommandAppManagementResult installRes = new GBDeviceCommandAppManagementResult();
                installRes.type = GBDeviceCommandAppManagementResult.CommandType.INSTALL;
                switch (pebbleCmd) {
                    case PUTBYTES_INIT:
                        installRes.token = buf.getInt();
                        installRes.result = GBDeviceCommandAppManagementResult.Result.SUCCESS;
                        break;
                    default:
                        installRes.token = buf.getInt();
                        installRes.result = GBDeviceCommandAppManagementResult.Result.FAILURE;
                        break;
                }
                cmd = installRes;
                break;
            case ENDPOINT_APPLICATIONMESSAGE:
                last_id = buf.get();
                long uuid_high = buf.getLong();
                long uuid_low = buf.getLong();

                switch (pebbleCmd) {
                    case APPLICATIONMESSAGE_PUSH:
                        UUID uuid = new UUID(uuid_high, uuid_low);
                        LOG.info("got APPLICATIONMESSAGE PUSH from UUID " + uuid);
                        if (WeatherNeatUUID.equals(uuid)) {
                            LOG.info("We know you, you are WeatherNeat");
                            GBDeviceCommandSendBytes sendBytes = new GBDeviceCommandSendBytes();
                            sendBytes.encodedBytes = encodeApplicationMessageWeatherNeatTest();
                            cmd = sendBytes;
                        } else if (MorpheuzSupport.uuid.equals(uuid)) {
                            ArrayList<Pair<Integer, Object>> dict = decodeDict(buf);
                            cmd = mMorpheuzSupport.handleMessage(dict);
                        }
                        break;
                    case APPLICATIONMESSAGE_ACK:
                        LOG.info("got APPLICATIONMESSAGE ACK");
                        break;
                    case APPLICATIONMESSAGE_NACK:
                        LOG.info("got APPLICATIONMESSAGE NACK");
                        break;
                    case APPLICATIONMESSAGE_REQUEST:
                        LOG.info("got APPLICATIONMESSAGE REQUEST");
                        break;
                    default:
                        break;
                }
                break;
            case ENDPOINT_DATALOG:
                if (pebbleCmd != DATALOG_TIMEOUT) {
                    byte id = buf.get();
                    LOG.info("DATALOG id " + id + " - sending 0x85 (ACK?)");
                    GBDeviceCommandSendBytes sendBytes = new GBDeviceCommandSendBytes();
                    sendBytes.encodedBytes = encodeDatalog(id, (byte) 0x85);
                    cmd = sendBytes;
                } else {
                    LOG.info("DATALOG TIMEOUT - ignoring");
                }
                break;
            case ENDPOINT_PHONEVERSION:
                switch (pebbleCmd) {
                    case PHONEVERSION_REQUEST:
                        LOG.info("Pebble asked for Phone/App Version - repLYING!");
                        GBDeviceCommandSendBytes sendBytes = new GBDeviceCommandSendBytes();
                        sendBytes.encodedBytes = encodePhoneVersion(PHONEVERSION_REMOTE_OS_ANDROID);
                        cmd = sendBytes;
                        break;
                    default:
                        break;
                }
                break;
            default:
                break;
        }

        return cmd;
    }
}
