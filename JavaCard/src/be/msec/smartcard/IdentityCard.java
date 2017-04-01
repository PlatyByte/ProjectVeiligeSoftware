package be.msec.smartcard;

import com.sun.javacard.crypto.e;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.JCSystem;
import javacard.framework.OwnerPIN;
import javacard.framework.Util;
import javacard.security.AESKey;
import javacard.security.Key;
import javacard.security.KeyBuilder;
import javacard.security.MessageDigest;
import javacard.security.PrivateKey;
import javacard.security.PublicKey;
import javacard.security.RSAPrivateKey;
import javacard.security.RSAPublicKey;
import javacard.security.RandomData;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

public class IdentityCard extends Applet {
	private final static byte IDENTITY_CARD_CLA = (byte) 0x80;

	private static final byte VALIDATE_PIN_INS = 0x22;
	private static final byte GET_NAME_INS = 0x24;
	private static final byte GET_SERIAL_INS = 0x26;
	private static final byte SIGN_INS = 0x28;
	private static final byte ASK_LENGTH_INS = 0x30;
	private static final byte GET_CERT_INS = 0x32;

	private static final byte SET_TEMPTIME_INS = 0x40;
	private static final byte VALIDATE_TIME_INS = 0x42;

	private static final byte SEND_SIG_INS = 0x46;
	private static final byte SEND_SIG_TIME_INS = 0x48;

	private static final byte SEND_CERT_INS = 0x50;
	private static final byte GET_KEY_INS = 0x52;
	private static final byte GET_MSG_INS = 0x54;

	private static final byte PUSH_MODULUS = 0x56;
	private static final byte PUSH_EXPONENT = 0x58;

	private static final byte GET_CHAL_INS = 0x72;
	private static final byte GET_ANSWER_CHAL_INS = 0x62;

	private static final byte SEND_REQ_ATT_INS = 0x66;
	private static final byte FETCH_REQ_ATT_INS = 0x68;

	private static final byte GET_ANSWER_CHAL_PART1 = 0x74;
	private static final byte GET_ANSWER_CHAL_PART2 = 0x76;
	private static final byte GET_ANSWER_CHAL_PART3 = 0x78;

	private static final byte FINAL_AUTH_INS = 0x64;

	private final static byte PIN_TRY_LIMIT = (byte) 0x03;
	private final static byte PIN_SIZE = (byte) 0x04;

	private final static short SW_VERIFICATION_FAILED = 0x6322;
	private final static short SW_PIN_VERIFICATION_REQUIRED = 0x6323;
	private final static short KAPPA = 0x6337;
	private final static short VERIFY_FAILED = 0x6338;
	private final static short ALG_FAILED = 0x6340;
	private final static short SEQUENTIAL_FAILURE = 0x6341;
	private final static short AUTH_FAILED = 0x6342;
	private final static short TYPE_UNKNOWN = 0x6343;
	private final static short INSUFFICIENT_RIGHTS = 0x6344;

	/** Invalid key ID. */
	public static final byte INVALID_KEY = (byte) -1;

	/** Invalid pair of key IDs. */
	public static final short INVALID_KEY_PAIR = (short) -1;

	// private RSAPublicKey pkMiddleware;
	private RSAPrivateKey secretKey;
	// private RSAPublicKey publicKey;

	private byte[] certServiceProvider;

	/** Holds random material for creating symmetric keys. */
	private static byte[] randomMaterial;

	private byte[] permsOverheid = new byte[] { (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 1, (byte) 0 };
	private byte[] permsSocial = new byte[] { (byte) 1, (byte) 0, (byte) 1, (byte) 0, (byte) 1, (byte) 1, (byte) 1 };
	private byte[] permsDefault = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0 };
	private byte[] permsKappa = new byte[] { (byte) 1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1 };

	// 86400 seconden ofwel 24 uur als threshold
	private byte[] threshold = new byte[] { (byte) 0, (byte) 1, (byte) 81, (byte) -128 };

	private byte[] serial = new byte[] { (byte) 0x4A, (byte) 0x61, (byte) 0x6e };
	private byte[] name = new byte[] { 0x4A, 0x61, 0x6E, 0x20, 0x56, 0x6F, 0x73, 0x73, 0x61, 0x65, 0x72, 0x74 };

	byte[] naam = new byte[] { (byte) 75, (byte) 97, (byte) 112, (byte) 112, (byte) 97, (byte) 75, (byte) 111,
			(byte) 110, (byte) 105, (byte) 110, (byte) 103, };

	byte[] adres = new byte[] { (byte) 71, (byte) 101, (byte) 98, (byte) 114, (byte) 111, (byte) 101, (byte) 100,
			(byte) 101, (byte) 114, (byte) 115, (byte) 100, (byte) 101, (byte) 115, (byte) 109, (byte) 101, (byte) 116,
			(byte) 32, (byte) 115, (byte) 116, (byte) 114, (byte) 97, (byte) 97, (byte) 116, (byte) 32, (byte) 49, };
	byte[] land = new byte[] { (byte) 66, (byte) 101, (byte) 108, (byte) 103, (byte) 105, (byte) 101, };
	byte[] geboorteDatum = new byte[] { (byte) 50, (byte) 48, (byte) 47, (byte) 48, (byte) 49, (byte) 47, (byte) 50,
			(byte) 48, (byte) 49, (byte) 55, };
	byte[] leeftijd = new byte[] { (byte) 49, (byte) 55, };
	byte[] geslacht = new byte[] { (byte) 68, (byte) 105, (byte) 100, (byte) 89, (byte) 111, (byte) 117, (byte) 74,
			(byte) 117, (byte) 115, (byte) 116, (byte) 65, (byte) 115, (byte) 115, (byte) 117, (byte) 109, (byte) 101,
			(byte) 77, (byte) 121, (byte) 71, (byte) 101, (byte) 110, (byte) 100, (byte) 101, (byte) 114, };

	byte[] foto = new byte[] { (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 12,
			(byte) 12, (byte) 12, (byte) 0, (byte) 35, (byte) 35, (byte) 35, (byte) 28, (byte) 37, (byte) 37, (byte) 37,
			(byte) -100, (byte) -127, (byte) -127, (byte) -127, (byte) -50, (byte) -126, (byte) -126, (byte) -126,
			(byte) -88, (byte) 101, (byte) 101, (byte) 101, (byte) -68, (byte) 106, (byte) 106, (byte) 106, (byte) -105,
			(byte) 109, (byte) 109, (byte) 109, (byte) 81, (byte) 107, (byte) 107, (byte) 107, (byte) 49, (byte) -78,
			(byte) -78, (byte) -78, (byte) 3, (byte) 83, (byte) 83, (byte) 83, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
			(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
			(byte) 0, (byte) 0, (byte) 34, (byte) 34, (byte) 34, (byte) 34, (byte) -91, (byte) 115, (byte) 115,
			(byte) 115, (byte) -28, (byte) 118, (byte) 118, (byte) 118, (byte) -1, (byte) 119, (byte) 119, (byte) 119,
			(byte) -1, (byte) 90, (byte) 90, (byte) 90, (byte) -1, (byte) 113, (byte) 113, (byte) 113, (byte) -1,
			(byte) 105, (byte) 105, (byte) 105, (byte) -1, (byte) 112, (byte) 112, (byte) 112, (byte) -1, (byte) 97,
			(byte) 97, (byte) 97, (byte) -1, (byte) -123, (byte) -123, (byte) -123, (byte) -76, (byte) -125,
			(byte) -125, (byte) -125, (byte) 40, (byte) 14, (byte) 14, (byte) 14, (byte) 0, (byte) 0, (byte) 0,
			(byte) 0, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 71, (byte) 63, (byte) 63, (byte) 63, (byte) -31,
			(byte) -103, (byte) -103, (byte) -103, (byte) -1, (byte) -99, (byte) -99, (byte) -99, (byte) -1,
			(byte) -103, (byte) -103, (byte) -103, (byte) -1, (byte) -104, (byte) -104, (byte) -104, (byte) -1,
			(byte) -127, (byte) -127, (byte) -127, (byte) -1, (byte) 103, (byte) 103, (byte) 103, (byte) -1, (byte) 92,
			(byte) 92, (byte) 92, (byte) -1, (byte) 74, (byte) 74, (byte) 74, (byte) -1, (byte) 66, (byte) 66,
			(byte) 66, (byte) -1, (byte) 111, (byte) 111, (byte) 111, (byte) -1, (byte) 80, (byte) 80, (byte) 80,
			(byte) -31, (byte) 53, (byte) 53, (byte) 53, (byte) 44, (byte) 71, (byte) 71, (byte) 71, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 53,
			(byte) -126, (byte) -126, (byte) -126, (byte) -9, (byte) -81, (byte) -81, (byte) -81, (byte) -1,
			(byte) -102, (byte) -102, (byte) -102, (byte) -1, (byte) -112, (byte) -112, (byte) -112, (byte) -1,
			(byte) -110, (byte) -110, (byte) -110, (byte) -1, (byte) 120, (byte) 120, (byte) 120, (byte) -1, (byte) 96,
			(byte) 96, (byte) 96, (byte) -1, (byte) 62, (byte) 62, (byte) 62, (byte) -1, (byte) 25, (byte) 25,
			(byte) 25, (byte) -1, (byte) 39, (byte) 39, (byte) 39, (byte) -1, (byte) 56, (byte) 56, (byte) 56,
			(byte) -1, (byte) 60, (byte) 60, (byte) 60, (byte) -1, (byte) 40, (byte) 40, (byte) 40, (byte) -1,
			(byte) 11, (byte) 11, (byte) 11, (byte) -85, (byte) 84, (byte) 84, (byte) 84, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -125,
			(byte) -108, (byte) -108, (byte) -108, (byte) -1, (byte) -85, (byte) -85, (byte) -85, (byte) -1,
			(byte) -108, (byte) -108, (byte) -108, (byte) -1, (byte) 103, (byte) 103, (byte) 103, (byte) -1, (byte) 80,
			(byte) 80, (byte) 80, (byte) -1, (byte) 64, (byte) 64, (byte) 64, (byte) -1, (byte) 35, (byte) 35,
			(byte) 35, (byte) -1, (byte) 56, (byte) 56, (byte) 56, (byte) -1, (byte) 53, (byte) 53, (byte) 53,
			(byte) -1, (byte) 84, (byte) 84, (byte) 84, (byte) -1, (byte) 97, (byte) 97, (byte) 97, (byte) -1,
			(byte) 52, (byte) 52, (byte) 52, (byte) -1, (byte) 26, (byte) 26, (byte) 26, (byte) -1, (byte) 3, (byte) 3,
			(byte) 3, (byte) -58, (byte) 29, (byte) 29, (byte) 29, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -68, (byte) 105, (byte) 105, (byte) 105,
			(byte) -1, (byte) -110, (byte) -110, (byte) -110, (byte) -1, (byte) 93, (byte) 93, (byte) 93, (byte) -1,
			(byte) 35, (byte) 35, (byte) 35, (byte) -1, (byte) 12, (byte) 12, (byte) 12, (byte) -1, (byte) 39,
			(byte) 39, (byte) 39, (byte) -1, (byte) 34, (byte) 34, (byte) 34, (byte) -1, (byte) 63, (byte) 63,
			(byte) 63, (byte) -1, (byte) -123, (byte) -123, (byte) -123, (byte) -1, (byte) -82, (byte) -82, (byte) -82,
			(byte) -1, (byte) -75, (byte) -75, (byte) -75, (byte) -1, (byte) -105, (byte) -105, (byte) -105, (byte) -1,
			(byte) 77, (byte) 77, (byte) 77, (byte) -1, (byte) 3, (byte) 3, (byte) 3, (byte) -31, (byte) 16, (byte) 16,
			(byte) 16, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) -36, (byte) 80, (byte) 80, (byte) 80, (byte) -1, (byte) 71, (byte) 71, (byte) 71,
			(byte) -1, (byte) 32, (byte) 32, (byte) 32, (byte) -1, (byte) 5, (byte) 5, (byte) 5, (byte) -1, (byte) 15,
			(byte) 15, (byte) 15, (byte) -1, (byte) 77, (byte) 77, (byte) 77, (byte) -1, (byte) 107, (byte) 107,
			(byte) 107, (byte) -1, (byte) -108, (byte) -108, (byte) -108, (byte) -1, (byte) -50, (byte) -50, (byte) -50,
			(byte) -1, (byte) -51, (byte) -51, (byte) -51, (byte) -1, (byte) -63, (byte) -63, (byte) -63, (byte) -1,
			(byte) -68, (byte) -68, (byte) -68, (byte) -1, (byte) -103, (byte) -103, (byte) -103, (byte) -1, (byte) 39,
			(byte) 39, (byte) 39, (byte) -33, (byte) 23, (byte) 23, (byte) 23, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -49, (byte) 51,
			(byte) 51, (byte) 51, (byte) -1, (byte) 25, (byte) 25, (byte) 25, (byte) -1, (byte) 12, (byte) 12,
			(byte) 12, (byte) -1, (byte) 29, (byte) 29, (byte) 29, (byte) -1, (byte) 68, (byte) 68, (byte) 68,
			(byte) -1, (byte) 123, (byte) 123, (byte) 123, (byte) -1, (byte) -76, (byte) -76, (byte) -76, (byte) -1,
			(byte) -65, (byte) -65, (byte) -65, (byte) -1, (byte) -42, (byte) -42, (byte) -42, (byte) -1, (byte) -58,
			(byte) -58, (byte) -58, (byte) -1, (byte) -68, (byte) -68, (byte) -68, (byte) -1, (byte) -77, (byte) -77,
			(byte) -77, (byte) -1, (byte) -94, (byte) -94, (byte) -94, (byte) -1, (byte) 106, (byte) 106, (byte) 106,
			(byte) -38, (byte) 28, (byte) 28, (byte) 28, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -83, (byte) 36, (byte) 36, (byte) 36, (byte) -1,
			(byte) 12, (byte) 12, (byte) 12, (byte) -1, (byte) 14, (byte) 14, (byte) 14, (byte) -1, (byte) 71,
			(byte) 71, (byte) 71, (byte) -1, (byte) -100, (byte) -100, (byte) -100, (byte) -1, (byte) -56, (byte) -56,
			(byte) -56, (byte) -1, (byte) -37, (byte) -37, (byte) -37, (byte) -1, (byte) -31, (byte) -31, (byte) -31,
			(byte) -1, (byte) -41, (byte) -41, (byte) -41, (byte) -1, (byte) -64, (byte) -64, (byte) -64, (byte) -1,
			(byte) -60, (byte) -60, (byte) -60, (byte) -1, (byte) -61, (byte) -61, (byte) -61, (byte) -1, (byte) -84,
			(byte) -84, (byte) -84, (byte) -1, (byte) -118, (byte) -118, (byte) -118, (byte) -44, (byte) 30, (byte) 30,
			(byte) 30, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 123, (byte) 45, (byte) 45, (byte) 45, (byte) -1, (byte) 8, (byte) 8, (byte) 8, (byte) -1,
			(byte) 16, (byte) 16, (byte) 16, (byte) -1, (byte) 124, (byte) 124, (byte) 124, (byte) -1, (byte) -37,
			(byte) -37, (byte) -37, (byte) -1, (byte) -29, (byte) -29, (byte) -29, (byte) -1, (byte) -47, (byte) -47,
			(byte) -47, (byte) -1, (byte) -109, (byte) -109, (byte) -109, (byte) -1, (byte) 120, (byte) 120, (byte) 120,
			(byte) -1, (byte) -98, (byte) -98, (byte) -98, (byte) -1, (byte) -87, (byte) -87, (byte) -87, (byte) -1,
			(byte) 124, (byte) 124, (byte) 124, (byte) -1, (byte) 100, (byte) 100, (byte) 100, (byte) -1, (byte) 112,
			(byte) 112, (byte) 112, (byte) -70, (byte) 63, (byte) 63, (byte) 63, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -114, (byte) -64,
			(byte) -64, (byte) -64, (byte) -1, (byte) 69, (byte) 69, (byte) 69, (byte) -1, (byte) 46, (byte) 46,
			(byte) 46, (byte) -1, (byte) -47, (byte) -47, (byte) -47, (byte) -1, (byte) -38, (byte) -38, (byte) -38,
			(byte) -1, (byte) -111, (byte) -111, (byte) -111, (byte) -1, (byte) 88, (byte) 88, (byte) 88, (byte) -1,
			(byte) 51, (byte) 51, (byte) 51, (byte) -1, (byte) 67, (byte) 67, (byte) 67, (byte) -1, (byte) -98,
			(byte) -98, (byte) -98, (byte) -1, (byte) -122, (byte) -122, (byte) -122, (byte) -1, (byte) 21, (byte) 21,
			(byte) 21, (byte) -1, (byte) 22, (byte) 22, (byte) 22, (byte) -1, (byte) 63, (byte) 63, (byte) 63,
			(byte) -117, (byte) 116, (byte) 116, (byte) 116, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) -114, (byte) -56, (byte) -56, (byte) -56,
			(byte) -1, (byte) -104, (byte) -104, (byte) -104, (byte) -1, (byte) 104, (byte) 104, (byte) 104, (byte) -1,
			(byte) -42, (byte) -42, (byte) -42, (byte) -1, (byte) -27, (byte) -27, (byte) -27, (byte) -1, (byte) -60,
			(byte) -60, (byte) -60, (byte) -1, (byte) -104, (byte) -104, (byte) -104, (byte) -1, (byte) -127,
			(byte) -127, (byte) -127, (byte) -1, (byte) -104, (byte) -104, (byte) -104, (byte) -1, (byte) -45,
			(byte) -45, (byte) -45, (byte) -1, (byte) -68, (byte) -68, (byte) -68, (byte) -1, (byte) 100, (byte) 100,
			(byte) 100, (byte) -1, (byte) 104, (byte) 104, (byte) 104, (byte) -1, (byte) -124, (byte) -124, (byte) -124,
			(byte) 120, (byte) -108, (byte) -108, (byte) -108, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 72, (byte) -29, (byte) -29, (byte) -29,
			(byte) -1, (byte) -68, (byte) -68, (byte) -68, (byte) -1, (byte) -51, (byte) -51, (byte) -51, (byte) -1,
			(byte) -45, (byte) -45, (byte) -45, (byte) -1, (byte) -31, (byte) -31, (byte) -31, (byte) -1, (byte) -15,
			(byte) -15, (byte) -15, (byte) -1, (byte) -19, (byte) -19, (byte) -19, (byte) -1, (byte) -32, (byte) -32,
			(byte) -32, (byte) -1, (byte) -39, (byte) -39, (byte) -39, (byte) -1, (byte) -34, (byte) -34, (byte) -34,
			(byte) -1, (byte) -37, (byte) -37, (byte) -37, (byte) -1, (byte) -99, (byte) -99, (byte) -99, (byte) -1,
			(byte) -82, (byte) -82, (byte) -82, (byte) -1, (byte) -92, (byte) -92, (byte) -92, (byte) 109, (byte) -115,
			(byte) -115, (byte) -115, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -23, (byte) -23, (byte) -23, (byte) -67, (byte) -23,
			(byte) -23, (byte) -23, (byte) -1, (byte) -24, (byte) -24, (byte) -24, (byte) -1, (byte) -47, (byte) -47,
			(byte) -47, (byte) -1, (byte) -44, (byte) -44, (byte) -44, (byte) -1, (byte) -46, (byte) -46, (byte) -46,
			(byte) -1, (byte) -43, (byte) -43, (byte) -43, (byte) -1, (byte) -57, (byte) -57, (byte) -57, (byte) -1,
			(byte) -65, (byte) -65, (byte) -65, (byte) -1, (byte) -41, (byte) -41, (byte) -41, (byte) -1, (byte) -38,
			(byte) -38, (byte) -38, (byte) -1, (byte) -100, (byte) -100, (byte) -100, (byte) -1, (byte) -101,
			(byte) -101, (byte) -101, (byte) -1, (byte) -119, (byte) -119, (byte) -119, (byte) 101, (byte) 123,
			(byte) 123, (byte) 123, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -34, (byte) -34, (byte) -34, (byte) 119, (byte) -37,
			(byte) -37, (byte) -37, (byte) -1, (byte) -49, (byte) -49, (byte) -49, (byte) -1, (byte) -45, (byte) -45,
			(byte) -45, (byte) -1, (byte) -46, (byte) -46, (byte) -46, (byte) -1, (byte) -61, (byte) -61, (byte) -61,
			(byte) -1, (byte) -83, (byte) -83, (byte) -83, (byte) -1, (byte) -80, (byte) -80, (byte) -80, (byte) -1,
			(byte) -68, (byte) -68, (byte) -68, (byte) -1, (byte) -113, (byte) -113, (byte) -113, (byte) -1, (byte) 123,
			(byte) 123, (byte) 123, (byte) -1, (byte) 104, (byte) 104, (byte) 104, (byte) -1, (byte) -127, (byte) -127,
			(byte) -127, (byte) -1, (byte) 119, (byte) 119, (byte) 119, (byte) 85, (byte) 118, (byte) 118, (byte) 118,
			(byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) -42, (byte) -42, (byte) -42, (byte) 0, (byte) -52, (byte) -52, (byte) -52, (byte) 68,
			(byte) -81, (byte) -81, (byte) -81, (byte) -5, (byte) -60, (byte) -60, (byte) -60, (byte) -1, (byte) -53,
			(byte) -53, (byte) -53, (byte) -1, (byte) -52, (byte) -52, (byte) -52, (byte) -1, (byte) -57, (byte) -57,
			(byte) -57, (byte) -1, (byte) -72, (byte) -72, (byte) -72, (byte) -1, (byte) -83, (byte) -83, (byte) -83,
			(byte) -1, (byte) -99, (byte) -99, (byte) -99, (byte) -1, (byte) 102, (byte) 102, (byte) 102, (byte) -1,
			(byte) 89, (byte) 89, (byte) 89, (byte) -1, (byte) -113, (byte) -113, (byte) -113, (byte) -6, (byte) -122,
			(byte) -122, (byte) -122, (byte) 42, (byte) 108, (byte) 108, (byte) 108, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0,
			(byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -74,
			(byte) -74, (byte) -74, (byte) 0, (byte) -76, (byte) -76, (byte) -76, (byte) 0, (byte) -86, (byte) -86,
			(byte) -86, (byte) -75, (byte) -91, (byte) -91, (byte) -91, (byte) -1, (byte) -79, (byte) -79, (byte) -79,
			(byte) -1, (byte) -71, (byte) -71, (byte) -71, (byte) -1, (byte) -65, (byte) -65, (byte) -65, (byte) -1,
			(byte) -71, (byte) -71, (byte) -71, (byte) -1, (byte) -85, (byte) -85, (byte) -85, (byte) -1, (byte) -122,
			(byte) -122, (byte) -122, (byte) -1, (byte) 103, (byte) 103, (byte) 103, (byte) -1, (byte) 110, (byte) 110,
			(byte) 110, (byte) -1, (byte) -118, (byte) -118, (byte) -118, (byte) -69, (byte) 112, (byte) 112,
			(byte) 112, (byte) 0, (byte) 109, (byte) 109, (byte) 109, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -106, (byte) -106,
			(byte) -106, (byte) 0, (byte) -106, (byte) -106, (byte) -106, (byte) 0, (byte) -104, (byte) -104,
			(byte) -104, (byte) 35, (byte) -106, (byte) -106, (byte) -106, (byte) -41, (byte) -109, (byte) -109,
			(byte) -109, (byte) -1, (byte) -108, (byte) -108, (byte) -108, (byte) -1, (byte) -106, (byte) -106,
			(byte) -106, (byte) -1, (byte) -58, (byte) -58, (byte) -58, (byte) -1, (byte) -62, (byte) -62, (byte) -62,
			(byte) -1, (byte) -104, (byte) -104, (byte) -104, (byte) -1, (byte) 126, (byte) 126, (byte) 126, (byte) -1,
			(byte) -115, (byte) -115, (byte) -115, (byte) -12, (byte) 107, (byte) 107, (byte) 107, (byte) 52,
			(byte) 105, (byte) 105, (byte) 105, (byte) 0, (byte) 110, (byte) 110, (byte) 110, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -104,
			(byte) -104, (byte) -104, (byte) 0, (byte) -104, (byte) -104, (byte) -104, (byte) 0, (byte) -104,
			(byte) -104, (byte) -104, (byte) 0, (byte) -103, (byte) -103, (byte) -103, (byte) 27, (byte) -112,
			(byte) -112, (byte) -112, (byte) -66, (byte) -123, (byte) -123, (byte) -123, (byte) -1, (byte) -107,
			(byte) -107, (byte) -107, (byte) -1, (byte) -81, (byte) -81, (byte) -81, (byte) -1, (byte) -66, (byte) -66,
			(byte) -66, (byte) -1, (byte) -71, (byte) -71, (byte) -71, (byte) -1, (byte) -94, (byte) -94, (byte) -94,
			(byte) -1, (byte) -121, (byte) -121, (byte) -121, (byte) 91, (byte) 100, (byte) 100, (byte) 100, (byte) 0,
			(byte) 104, (byte) 104, (byte) 104, (byte) 0, (byte) 104, (byte) 104, (byte) 104, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1,
			(byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -104,
			(byte) -104, (byte) -104, (byte) 0, (byte) -104, (byte) -104, (byte) -104, (byte) 0, (byte) -104,
			(byte) -104, (byte) -104, (byte) 0, (byte) -104, (byte) -104, (byte) -104, (byte) 0, (byte) -110,
			(byte) -110, (byte) -110, (byte) 5, (byte) -122, (byte) -122, (byte) -122, (byte) 97, (byte) -125,
			(byte) -125, (byte) -125, (byte) -66, (byte) 127, (byte) 127, (byte) 127, (byte) -32, (byte) 126,
			(byte) 126, (byte) 126, (byte) -20, (byte) 125, (byte) 125, (byte) 125, (byte) -27, (byte) 109, (byte) 109,
			(byte) 109, (byte) 100, (byte) 86, (byte) 86, (byte) 86, (byte) 0, (byte) 87, (byte) 87, (byte) 87,
			(byte) 0, (byte) 88, (byte) 88, (byte) 88, (byte) 0, (byte) 88, (byte) 88, (byte) 88, (byte) 0, (byte) -1,
			(byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1, (byte) -1, (byte) 0, (byte) -1, (byte) -1,
			(byte) -1 };

	private byte[] timePubExp = new byte[] { (byte) 1, (byte) 0, (byte) 1 };
	private byte[] timePubMod = new byte[] { (byte) -17, (byte) -49, (byte) 3, (byte) -29, (byte) -86, (byte) 74,
			(byte) 61, (byte) -60, (byte) 101, (byte) -54, (byte) -76, (byte) 23, (byte) -75, (byte) 63, (byte) -88,
			(byte) 115, (byte) -93, (byte) -78, (byte) -22, (byte) -23, (byte) -74, (byte) 80, (byte) 73, (byte) -127,
			(byte) 89, (byte) -89, (byte) -77, (byte) -48, (byte) 8, (byte) 78, (byte) -104, (byte) 114, (byte) -65,
			(byte) -71, (byte) -117, (byte) -56, (byte) -126, (byte) 54, (byte) 69, (byte) -120, (byte) -75, (byte) 112,
			(byte) -35, (byte) 30, (byte) -71, (byte) -65, (byte) 98, (byte) 112, (byte) 107, (byte) 117, (byte) -10,
			(byte) 60, (byte) -44, (byte) -34, (byte) -119, (byte) 107, (byte) 74, (byte) 26, (byte) 74, (byte) 56,
			(byte) -43, (byte) -79, (byte) 113, (byte) 49 };

	private byte[] coSecExp = new byte[] { (byte) 94, (byte) -55, (byte) 95, (byte) 12, (byte) 106, (byte) -122,
			(byte) -23, (byte) -30, (byte) 15, (byte) -36, (byte) -110, (byte) -14, (byte) -55, (byte) -38, (byte) -115,
			(byte) -123, (byte) -96, (byte) -28, (byte) 5, (byte) 85, (byte) -109, (byte) -121, (byte) 50, (byte) -63,
			(byte) -100, (byte) 72, (byte) -128, (byte) 27, (byte) -58, (byte) 88, (byte) -100, (byte) -8, (byte) 12,
			(byte) -108, (byte) -11, (byte) 117, (byte) -64, (byte) -119, (byte) 120, (byte) 46, (byte) 90, (byte) 4,
			(byte) 57, (byte) 13, (byte) -109, (byte) 30, (byte) -32, (byte) -82, (byte) 10, (byte) -66, (byte) 26,
			(byte) -81, (byte) 37, (byte) -114, (byte) -94, (byte) -7, (byte) -21, (byte) 40, (byte) 69, (byte) -67,
			(byte) 8, (byte) 92, (byte) 64, (byte) -55 };
	private byte[] coSecMod = new byte[] { (byte) -36, (byte) -33, (byte) 70, (byte) -88, (byte) -101, (byte) 15,
			(byte) 50, (byte) 1, (byte) -28, (byte) 20, (byte) -122, (byte) 22, (byte) -112, (byte) 7, (byte) 14,
			(byte) -63, (byte) -96, (byte) 80, (byte) 114, (byte) -114, (byte) -111, (byte) 29, (byte) 91, (byte) 117,
			(byte) 23, (byte) 98, (byte) 118, (byte) 89, (byte) 127, (byte) 71, (byte) -89, (byte) -44, (byte) -71,
			(byte) -65, (byte) 74, (byte) 71, (byte) 12, (byte) -9, (byte) 91, (byte) 43, (byte) -109, (byte) 118,
			(byte) 56, (byte) -128, (byte) 90, (byte) -106, (byte) 0, (byte) 5, (byte) -5, (byte) 14, (byte) 117,
			(byte) -27, (byte) 56, (byte) -73, (byte) -11, (byte) -62, (byte) 18, (byte) 102, (byte) 81, (byte) -124,
			(byte) 60, (byte) 14, (byte) 77, (byte) -33 };
	private byte[] coCert = new byte[] { (byte) 48, (byte) -126, (byte) 1, (byte) -121, (byte) 48, (byte) -126,
			(byte) 1, (byte) 49, (byte) 2, (byte) 1, (byte) 1, (byte) 48, (byte) 13, (byte) 6, (byte) 9, (byte) 42,
			(byte) -122, (byte) 72, (byte) -122, (byte) -9, (byte) 13, (byte) 1, (byte) 1, (byte) 11, (byte) 5,
			(byte) 0, (byte) 48, (byte) 72, (byte) 49, (byte) 11, (byte) 48, (byte) 9, (byte) 6, (byte) 3, (byte) 85,
			(byte) 4, (byte) 6, (byte) 19, (byte) 2, (byte) 66, (byte) 69, (byte) 49, (byte) 19, (byte) 48, (byte) 17,
			(byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 8, (byte) 12, (byte) 10, (byte) 83, (byte) 111, (byte) 109,
			(byte) 101, (byte) 45, (byte) 83, (byte) 116, (byte) 97, (byte) 116, (byte) 101, (byte) 49, (byte) 17,
			(byte) 48, (byte) 15, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 10, (byte) 12, (byte) 8, (byte) 67,
			(byte) 101, (byte) 114, (byte) 116, (byte) 65, (byte) 117, (byte) 116, (byte) 104, (byte) 49, (byte) 17,
			(byte) 48, (byte) 15, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 3, (byte) 12, (byte) 8, (byte) 67,
			(byte) 101, (byte) 114, (byte) 116, (byte) 65, (byte) 117, (byte) 116, (byte) 104, (byte) 48, (byte) 30,
			(byte) 23, (byte) 13, (byte) 49, (byte) 55, (byte) 48, (byte) 51, (byte) 50, (byte) 55, (byte) 49,
			(byte) 50, (byte) 53, (byte) 48, (byte) 50, (byte) 56, (byte) 90, (byte) 23, (byte) 13, (byte) 49,
			(byte) 57, (byte) 48, (byte) 51, (byte) 50, (byte) 55, (byte) 49, (byte) 50, (byte) 53, (byte) 48,
			(byte) 50, (byte) 56, (byte) 90, (byte) 48, (byte) 85, (byte) 49, (byte) 11, (byte) 48, (byte) 9, (byte) 6,
			(byte) 3, (byte) 85, (byte) 4, (byte) 6, (byte) 19, (byte) 2, (byte) 66, (byte) 69, (byte) 49, (byte) 19,
			(byte) 48, (byte) 17, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 8, (byte) 12, (byte) 10, (byte) 83,
			(byte) 111, (byte) 109, (byte) 101, (byte) 45, (byte) 83, (byte) 116, (byte) 97, (byte) 116, (byte) 101,
			(byte) 49, (byte) 15, (byte) 48, (byte) 13, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 10, (byte) 12,
			(byte) 6, (byte) 67, (byte) 111, (byte) 109, (byte) 109, (byte) 111, (byte) 110, (byte) 49, (byte) 15,
			(byte) 48, (byte) 13, (byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 11, (byte) 12, (byte) 6, (byte) 67,
			(byte) 111, (byte) 109, (byte) 109, (byte) 111, (byte) 110, (byte) 49, (byte) 15, (byte) 48, (byte) 13,
			(byte) 6, (byte) 3, (byte) 85, (byte) 4, (byte) 3, (byte) 12, (byte) 6, (byte) 67, (byte) 111, (byte) 109,
			(byte) 109, (byte) 111, (byte) 110, (byte) 48, (byte) 92, (byte) 48, (byte) 13, (byte) 6, (byte) 9,
			(byte) 42, (byte) -122, (byte) 72, (byte) -122, (byte) -9, (byte) 13, (byte) 1, (byte) 1, (byte) 1,
			(byte) 5, (byte) 0, (byte) 3, (byte) 75, (byte) 0, (byte) 48, (byte) 72, (byte) 2, (byte) 65, (byte) 0,
			(byte) -36, (byte) -33, (byte) 70, (byte) -88, (byte) -101, (byte) 15, (byte) 50, (byte) 1, (byte) -28,
			(byte) 20, (byte) -122, (byte) 22, (byte) -112, (byte) 7, (byte) 14, (byte) -63, (byte) -96, (byte) 80,
			(byte) 114, (byte) -114, (byte) -111, (byte) 29, (byte) 91, (byte) 117, (byte) 23, (byte) 98, (byte) 118,
			(byte) 89, (byte) 127, (byte) 71, (byte) -89, (byte) -44, (byte) -71, (byte) -65, (byte) 74, (byte) 71,
			(byte) 12, (byte) -9, (byte) 91, (byte) 43, (byte) -109, (byte) 118, (byte) 56, (byte) -128, (byte) 90,
			(byte) -106, (byte) 0, (byte) 5, (byte) -5, (byte) 14, (byte) 117, (byte) -27, (byte) 56, (byte) -73,
			(byte) -11, (byte) -62, (byte) 18, (byte) 102, (byte) 81, (byte) -124, (byte) 60, (byte) 14, (byte) 77,
			(byte) -33, (byte) 2, (byte) 3, (byte) 1, (byte) 0, (byte) 1, (byte) 48, (byte) 13, (byte) 6, (byte) 9,
			(byte) 42, (byte) -122, (byte) 72, (byte) -122, (byte) -9, (byte) 13, (byte) 1, (byte) 1, (byte) 11,
			(byte) 5, (byte) 0, (byte) 3, (byte) 65, (byte) 0, (byte) 99, (byte) -110, (byte) 69, (byte) -43,
			(byte) -84, (byte) 34, (byte) -83, (byte) -65, (byte) 10, (byte) 81, (byte) -23, (byte) -21, (byte) -32,
			(byte) -43, (byte) 1, (byte) 20, (byte) 98, (byte) -128, (byte) -40, (byte) 92, (byte) -54, (byte) 124,
			(byte) 48, (byte) 29, (byte) 98, (byte) -52, (byte) 47, (byte) 114, (byte) 91, (byte) -22, (byte) -35,
			(byte) -80, (byte) 98, (byte) -75, (byte) -9, (byte) 48, (byte) 115, (byte) -57, (byte) -109, (byte) 62,
			(byte) -31, (byte) -18, (byte) -2, (byte) -101, (byte) 79, (byte) 78, (byte) 3, (byte) 86, (byte) 79,
			(byte) 7, (byte) 50, (byte) -34, (byte) -39, (byte) -86, (byte) -23, (byte) 80, (byte) 5, (byte) -117,
			(byte) -119, (byte) 112, (byte) -38, (byte) 36, (byte) -41, (byte) -58 };

	private RSAPublicKey timePublicKey;
	private RSAPrivateKey coPrivateKey;
	private OwnerPIN pin;

	private short signLength;
	private byte[] sign;

	private byte[] lastTime;
	private byte[] tempTime;

	private byte[] tempTimeUpdate;

	private short privKeyKs;

	private byte[] request;

	/**
	 * Randomizer instance.
	 * 
	 * Static because the dynamic version leaks memory.
	 */
	private static RandomData randomizer;

	/** The key file (parallel: register file). */
	private static Key[] keys;

	/** The number of entries in the keys file. */
	public static final short NUM_KEYS = 8;

	/** Certificate RSA Public key. */
	private RSAPublicKey pubCertKey = null;

	private byte[] pubCertExponent;
	private byte[] pubCertModulus;

	private byte messageChallenge;
	private byte[] endStepThree;

	private byte authenticated;

	private IdentityCard() {
		/* During instantiation of the applet, all objects are created. */
		pin = new OwnerPIN(PIN_TRY_LIMIT, PIN_SIZE);
		pin.update(new byte[] { 0x01, 0x02, 0x03, 0x04 }, (short) 0, PIN_SIZE);

		// initial time
		lastTime = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0 };

		short offset = 0;
		short keySizeInBytes = (short) 64;
		short keySizeInBits = (short) (keySizeInBytes * 8);
		timePublicKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, keySizeInBits, false);
		timePublicKey.setExponent(timePubExp, offset, (short) 3);
		timePublicKey.setModulus(timePubMod, offset, keySizeInBytes);

		keySizeInBytes = 64;
		keySizeInBits = (short) (keySizeInBytes * 8);
		coPrivateKey = (RSAPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PRIVATE, keySizeInBits, false);
		coPrivateKey.setExponent(coSecExp, offset, keySizeInBytes);
		coPrivateKey.setModulus(coSecMod, offset, keySizeInBytes);

		randomMaterial = JCSystem.makeTransientByteArray((short) 16, JCSystem.CLEAR_ON_DESELECT);
		keys = new Key[NUM_KEYS];
		randomizer = RandomData.getInstance(RandomData.ALG_PSEUDO_RANDOM);

		/* This method registers the applet with the JCRE on the card. */
		register();
	}

	/*
	 * This method is called by the JCRE when installing the applet on the card.
	 */
	public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
		new IdentityCard();
	}

	/*
	 * If no tries are remaining, the applet refuses selection. The card can,
	 * therefore, no longer be used for identification.
	 */
	public boolean select() {
		if (pin.getTriesRemaining() == 0)
			return false;
		return true;
	}

	/*
	 * This method is called when the applet is selected and an APDU arrives.
	 */
	public void process(APDU apdu) throws ISOException {
		// A reference to the buffer, where the APDU data is stored, is
		// retrieved.
		byte[] buffer = apdu.getBuffer();

		// If the APDU selects the applet, no further processing is required.
		if (this.selectingApplet())
			return;

		// Check whether the indicated class of instructions is compatible with
		// this applet.
		if (buffer[ISO7816.OFFSET_CLA] != IDENTITY_CARD_CLA)
			ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
		// A switch statement is used to select a method depending on the
		// instruction
		switch (buffer[ISO7816.OFFSET_INS]) {
		case VALIDATE_PIN_INS:
			validatePIN(apdu);
			break;
		case GET_SERIAL_INS:
			getSerial(apdu);
			break;
		case GET_NAME_INS:
			getName(apdu);
			break;
		case SIGN_INS:
			signSomething(apdu);
			break;
		case ASK_LENGTH_INS:
			askLength(apdu);
			break;
		case GET_CERT_INS:
			askCertificate(apdu);
			break;
		case SET_TEMPTIME_INS:
			setTempTime(apdu);
			break;
		case VALIDATE_TIME_INS:
			validateTime(apdu);
			break;
		case SEND_SIG_INS:
			updateSig(apdu);
			break;
		case SEND_SIG_TIME_INS:
			updateTime(apdu);
			break;
		case SEND_CERT_INS:
			receiveCert(apdu);
			break;
		case GET_KEY_INS:
			generateKey(apdu);
			break;
		case GET_MSG_INS:
			fetchMessage(apdu);
			break;
		case PUSH_EXPONENT:
			receiveExponent(apdu);
			break;
		case PUSH_MODULUS:
			receiveModulus(apdu);
			break;
		case GET_CHAL_INS:
			getChallenge(apdu);
			break;
		case FINAL_AUTH_INS:
			validateFinalAuth(apdu);
			break;
		case GET_ANSWER_CHAL_PART1:
			returnAnswerChallenge(apdu, (short) 0);
			break;
		case GET_ANSWER_CHAL_PART2:
			returnAnswerChallenge(apdu, (short) 1);
			break;
		case GET_ANSWER_CHAL_PART3:
			returnAnswerChallenge(apdu, (short) 2);
			break;
		case SEND_REQ_ATT_INS:
			sendReqAttributes(apdu);
			break;
		case FETCH_REQ_ATT_INS:
			fetchResponseReqAttributes(apdu);
			break;
		default:
			ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}
	}

	private void returnAnswerChallenge(APDU apdu, short j) {
		short start = (short) (0 + j * 250);
		byte[] result = new byte[250];
		for (short i = 0; i < (short) 250; i++) {
			short index = (short) (i + 250 * j);
			result[i] = endStepThree[index];
		}
		if (j == (short) 2) {
			endStepThree = null;
		}

		apdu.setOutgoing();
		apdu.setOutgoingLength((short) result.length);
		apdu.sendBytesLong(result, (short) 0, (short) result.length);
	}

	/*
	 * This method is used to authenticate the owner of the card using a PIN
	 * code.
	 */
	private void validatePIN(APDU apdu) {
		// shizzle in commentaar is om te werken adhv encrypted communication

		// byte[] pinBytesWithNull = receiveBytesEncryptedByMW(apdu);
		// byte[] pinBytes = removeNullBytes(pinBytesWithNull);
		byte[] buffer = apdu.getBuffer();

		if (buffer[ISO7816.OFFSET_LC] == PIN_SIZE) {
			apdu.setIncomingAndReceive();
			if (pin.check(buffer, ISO7816.OFFSET_CDATA, PIN_SIZE) == false)
				ISOException.throwIt(SW_VERIFICATION_FAILED);
		} else
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
	}

	/*
	 * This method checks whether the user is authenticated and sends the serial
	 * number.
	 */
	private void getSerial(APDU apdu) {
		// If the pin is not validated, a response APDU with the
		// 'SW_PIN_VERIFICATION_REQUIRED' status word is transmitted.
		if (!pin.isValidated())
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else {
			apdu.setOutgoing();
			apdu.setOutgoingLength((short) serial.length);
			apdu.sendBytesLong(serial, (short) 0, (short) serial.length);
		}
	}

	private void updateTime(APDU apdu) {
		if (tempTimeUpdate == null) {
			byte[] buffer = apdu.getBuffer();
			apdu.setIncomingAndReceive();
			byte[] time = slice(buffer, ISO7816.OFFSET_CDATA, (short) buffer.length);
			time = slice(time, (short) 0, (short) 4);
			tempTimeUpdate = time;
		} else {
			ISOException.throwIt(SEQUENTIAL_FAILURE);
		}
	}

	private void getChallenge(APDU apdu) {
		if (endStepThree == null) {
			byte[] buffer = apdu.getBuffer();
			apdu.setIncomingAndReceive();
			byte[] challenge = slice(buffer, ISO7816.OFFSET_CDATA, (short) buffer.length);
			challenge = cutOffNulls(challenge);

			if (authenticated == (byte) 1) {

				Cipher symCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
				AESKey ks = (AESKey) keys[privKeyKs];
				symCipher.init(ks, Cipher.MODE_DECRYPT);

				byte[] decryptedData = new byte[16];
				short dataLen = (short) challenge.length;
				symCipher.doFinal(challenge, (short) 0, dataLen, decryptedData, (short) 0);
				decryptedData = cutOffNulls(decryptedData);

				sign = new byte[240];
				byte[] hash = hash(decryptedData);
				hash = cutOffNulls(hash);
				short signLength = generateSignature(coPrivateKey, decryptedData, (short) 0, (short) 1, sign);
				sign = cutOffNulls(sign);

				short totLength = (short) (coCert.length + signLength + 1);
				byte[] eMsg = new byte[totLength];
				eMsg[0] = (byte) (signLength & 0xff);
				for (short i = 0; i < signLength; i++) {
					short index = (short) (i + 1);
					eMsg[index] = sign[i];
				}
				short halfway = (short) (1 + signLength);
				short certLength = (short) coCert.length;
				for (short i = 0; i < certLength; i++) {
					short index = (short) (i + halfway);
					eMsg[index] = coCert[i];
				}
				byte[] result = new byte[512];
				for (short i = 0; i < totLength; i++) {
					result[i] = eMsg[i];
				}
				eMsg = result;

				symCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
				symCipher.init(keys[privKeyKs], Cipher.MODE_ENCRYPT);

				byte[] encryptedData = new byte[750];
				try {
					symCipher.doFinal(eMsg, (short) 0, (short) eMsg.length, encryptedData, (short) 0);
				} catch (Exception e) {
					ISOException.throwIt(ALG_FAILED);
				}
				byte[] b = cutOffNulls(encryptedData);
				endStepThree = encryptedData;
				ISOException.throwIt(KAPPA);
			} else {
				ISOException.throwIt(AUTH_FAILED);
			}
		} else {
			ISOException.throwIt(SEQUENTIAL_FAILURE);
		}
	}

	private byte[] hash(byte[] b) {
		byte[] returnData = new byte[64];
		MessageDigest digest = MessageDigest.getInstance(MessageDigest.ALG_SHA, false);
		digest.doFinal(b, (short) 0, (short) b.length, returnData, (short) 0);
		return returnData;
	}

	private void updateSig(APDU apdu) {
		if (tempTimeUpdate != null) {
			byte[] buffer = apdu.getBuffer();
			byte[] incomingData = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
			short bytesLeft;
			short readCount;
			short offSet = 0x00;

			bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
			readCount = apdu.setIncomingAndReceive();
			while (bytesLeft > 0) {
				Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, incomingData, offSet, readCount);
				bytesLeft -= readCount;
				offSet += readCount;
				readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
			}

			// incomingData = cutOffNulls(incomingData);
			byte[] signature = new byte[64];
			for (short i = 0; i < 64; i++) {
				signature[i] = incomingData[i];
			}

			boolean verified = verifyPublic(timePublicKey, tempTimeUpdate, signature);
			if (verified) {
				boolean past = checkIfPast(lastTime, tempTimeUpdate);
				if (past)
					ISOException.throwIt(VERIFY_FAILED);
				lastTime = tempTimeUpdate;
				tempTimeUpdate = null;
				ISOException.throwIt(KAPPA);
			} else {
				ISOException.throwIt(VERIFY_FAILED);
			}

		} else {
			ISOException.throwIt(SEQUENTIAL_FAILURE);
		}
	}

	private void getName(APDU apdu) {
		// If the pin is not validated, a response APDU with the
		// 'SW_PIN_VERIFICATION_REQUIRED' status word is transmitted.
		if (!pin.isValidated())
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else {
			apdu.setOutgoing();
			apdu.setOutgoingLength((short) name.length);
			apdu.sendBytesLong(name, (short) 0, (short) name.length);
		}
	}

	public void signSomething(APDU apdu) {
		byte[] buffer = apdu.getBuffer();
		byte[] output = new byte[240];
		short siglength = generateSignature(secretKey, buffer, ISO7816.OFFSET_CDATA, apdu.setIncomingAndReceive(),
				output);

		apdu.setOutgoing();
		apdu.setOutgoingLength(siglength);
		apdu.sendBytesLong(output, (short) 0, (short) siglength);

	}

	private void receiveCert(APDU apdu) {
		byte[] buffer = apdu.getBuffer();

		// short teller = (short) (buffer[ISO7816.OFFSET_P1] & (short) 0xFF); //
		// test?

		byte[] incomingData = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		short offSet = 0x00;
		short bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		short readCount = apdu.setIncomingAndReceive();
		while (bytesLeft > 0) {
			Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, incomingData, offSet, readCount);
			bytesLeft -= readCount;
			offSet += readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}

		certServiceProvider = new byte[(short) incomingData.length];
		Util.arrayCopy(incomingData, (short) 0, certServiceProvider, (short) 0, (short) incomingData.length);
		certServiceProvider = cutOffNulls(certServiceProvider);

	}

	private void receiveModulus(APDU apdu) {

		byte[] buffer = apdu.getBuffer();
		byte[] incomingData = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		short bytesLeft;
		short readCount;
		short offSet = 0x00;

		bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		readCount = apdu.setIncomingAndReceive();
		while (bytesLeft > 0) {
			Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, incomingData, offSet, readCount);
			bytesLeft -= readCount;
			offSet += readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}

		pubCertModulus = cutOffNulls(incomingData);

	}

	private void receiveExponent(APDU apdu) {

		byte[] buffer = apdu.getBuffer();
		byte[] incomingData = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		short bytesLeft;
		short readCount;
		short offSet = 0x00;

		bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		readCount = apdu.setIncomingAndReceive();
		while (bytesLeft > 0) {
			Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, incomingData, offSet, readCount);
			bytesLeft -= readCount;
			offSet += readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}

		pubCertExponent = cutOffNulls(incomingData);

	}

	private void sendReqAttributes(APDU apdu) {
		// If the pin is not validated, a response APDU with the
		// 'SW_PIN_VERIFICATION_REQUIRED' status word is transmitted.
		if (!pin.isValidated())
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else {
			if (authenticated == (byte) 1) {
				byte[] buffer = apdu.getBuffer();
				byte[] incomingData = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
				short bytesLeft;
				short readCount;
				short offSet = 0x00;

				bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
				readCount = apdu.setIncomingAndReceive();
				while (bytesLeft > 0) {
					Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, incomingData, offSet, readCount);
					bytesLeft -= readCount;
					offSet += readCount;
					readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
				}

				request = incomingData;
			} else {
				ISOException.throwIt(AUTH_FAILED);
			}
		}
	}

	private void fetchResponseReqAttributes(APDU apdu) {
		// If the pin is not validated, a response APDU with the
		// 'SW_PIN_VERIFICATION_REQUIRED' status word is transmitted.
		if (!pin.isValidated())
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else {
			// TODO FETCH P1
			if (request == null || authenticated != (byte) 1)
				ISOException.throwIt(AUTH_FAILED);

			byte type = certServiceProvider[(short) (64 + 3 + 3 + 35)];
			byte[] rechten = null;
			if (type == (byte) 71) {
				rechten = permsOverheid;
			} else if (type == (byte) 83) {
				rechten = permsSocial;
			} else if (type == (byte) 68) {
				rechten = permsDefault;
			} else if (type == (byte) 79) {
				rechten = permsKappa;
			} else {
				ISOException.throwIt(TYPE_UNKNOWN);
			}

			short i;
			for (i = 0; i < 7; i++) {
				if (request[i] > rechten[i])
					ISOException.throwIt(INSUFFICIENT_RIGHTS);
			}

			apdu.setOutgoing();
			apdu.setOutgoingLength((short) serial.length);
			apdu.sendBytesLong(serial, (short) 0, (short) serial.length);
		}
	}

	public void generateKey(APDU apdu) {

		short keySizeInBytes = (short) 64;
		short keySizeInBits = (short) (keySizeInBytes * 8);
		pubCertKey = (RSAPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_RSA_PUBLIC, keySizeInBits, false);
		pubCertKey.setExponent(certServiceProvider, (short) 64, (short) 3);
		pubCertKey.setModulus(certServiceProvider, (short) 0, (short) 64);

		// TODO if (verifyCert(CertSP)==false) abort()
		// TODO if (CertSP.validEndTime < lastValidationTime) abort()

		// DONE Ks := genNewSymKey(getSecureRand())
		byte privKeyIndex = findFreeKeySlot();
		if (privKeyIndex == INVALID_KEY)
			ISOException.throwIt(INVALID_KEY_PAIR);

		AESKey Ks = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
		short keySize = (short) (Ks.getSize() / 8);
		random(randomMaterial, (short) 0, keySize);
		Ks.setKey(randomMaterial, (short) 0);
		keys[privKeyIndex] = Ks;
		privKeyKs = privKeyIndex;

		// DONE Ekey := asymEncrypt(Ks, CertSP.PKSP)
		Cipher asymCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
		asymCipher.init(pubCertKey, Cipher.MODE_ENCRYPT);
		byte[] encryptedData = new byte[256];
		byte[] KsInBytes = new byte[16];
		Ks.getKey(KsInBytes, (short) 0);

		KsInBytes = slice(KsInBytes, (short) 0, (short) 16);

		try {
			asymCipher.doFinal(KsInBytes, (short) 0, (short) 16, encryptedData, (short) 0);
		} catch (Exception e) {
			ISOException.throwIt(ALG_FAILED);
		}

		// DONE return EKey
		apdu.setOutgoing();
		apdu.setOutgoingLength((short) encryptedData.length);
		apdu.sendBytesLong(encryptedData, (short) 0, (short) encryptedData.length);

	}

	public void fetchMessage(APDU apdu) {

		byte[] buffer = apdu.getBuffer();

		// DONE c := genChallenge(getSecureRand())
		byte[] c = new byte[1];
		random(c, (short) 0, (short) 1);

		messageChallenge = c[0];

		// byte certSubject = (byte) (buffer[ISO7816.OFFSET_P1] & (short) 0xFF);
		// // test?

		byte certSubject = certServiceProvider[(short) (64 + 3 + 3)];
		// DONE Emsg := symEncrypt([c, CertSP:Subject], Ks)
		Cipher symCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		AESKey ks = (AESKey) keys[privKeyKs];
		byte[] b = new byte[1024];
		byte d = ks.getKey(b, (short) 0);
		symCipher.init(ks, Cipher.MODE_ENCRYPT);
		byte[] encryptedData = new byte[16];
		byte[] sendData = new byte[16];
		sendData[0] = c[0];
		sendData[1] = certSubject;

		try {
			symCipher.doFinal(sendData, (short) 0, (short) sendData.length, encryptedData, (short) 0);
		} catch (Exception e) {
			ISOException.throwIt(ALG_FAILED);
		}

		// DONE return EKey
		apdu.setOutgoing();
		apdu.setOutgoingLength((short) encryptedData.length);
		apdu.sendBytesLong(encryptedData, (short) 0, (short) encryptedData.length);

	}

	public void validateFinalAuth(APDU apdu) {

		byte[] buffer = apdu.getBuffer();
		byte[] incomingData = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
		short bytesLeft;
		short readCount;
		short offSet = 0x00;

		bytesLeft = (short) (buffer[ISO7816.OFFSET_LC] & 0x00FF);
		readCount = apdu.setIncomingAndReceive();
		while (bytesLeft > 0) {
			Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, incomingData, offSet, readCount);
			bytesLeft -= readCount;
			offSet += readCount;
			readCount = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
		}

		byte[] response = cutOffNulls(incomingData);

		Cipher symCipher = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
		symCipher.init(keys[privKeyKs], Cipher.MODE_DECRYPT);

		byte[] decryptedData = new byte[16];
		symCipher.doFinal(response, (short) 0, (short) response.length, decryptedData, (short) 0);
		decryptedData = cutOffNulls(decryptedData);

		if (messageChallenge + 1 == decryptedData[0]) {
			authenticated = (byte) 1;
		} else {
			authenticated = (byte) 0;
			ISOException.throwIt(AUTH_FAILED);
		}

	}

	public void setTempTime(APDU apdu) {

		byte[] buffer = apdu.getBuffer();

		if (tempTime == null) {
			tempTime = new byte[4];

			tempTime[0] = (byte) (buffer[ISO7816.OFFSET_P1] & (short) 0xFF); // test?
			tempTime[1] = (byte) (buffer[ISO7816.OFFSET_P2] & (short) 0xFF); // test?
		} else {
			tempTime[2] = (byte) (buffer[ISO7816.OFFSET_P1] & (short) 0xFF); // test?
			tempTime[3] = (byte) (buffer[ISO7816.OFFSET_P2] & (short) 0xFF); // test?
		}

	}

	public void validateTime(APDU apdu) {

		// If current - last(buffer) > threshold send 0 else 1
		boolean refresh = compareTime();

		byte[] answer;

		if (refresh) {
			answer = new byte[] { (byte) 0 };
		} else {
			answer = new byte[] { (byte) 1 };
		}

		tempTime = null;

		apdu.setOutgoing();
		apdu.setOutgoingLength((short) answer.length);
		apdu.sendBytesLong(answer, (short) 0, (short) answer.length);
	}

	public void askLength(APDU apdu) {

		// certificate.length is short --> bv 240
		// byte 128
		// 240 / 128 --> 1 // rest: 112
		// resp[0] = 1 en resp[1] = 112

		byte[] response = new byte[2];
		response[0] = (byte) (certServiceProvider.length / 100);
		response[1] = (byte) (certServiceProvider.length % 100);
		apdu.setOutgoing();
		apdu.setOutgoingLength((short) response.length);
		apdu.sendBytesLong(response, (short) 0, (short) response.length);

	}

	public void askCertificate(APDU apdu) {
		if (!pin.isValidated())
			ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
		else {

			byte[] buffer = apdu.getBuffer();
			byte[] output = new byte[240];

			short teller = (short) (buffer[ISO7816.OFFSET_P1] & (short) 0xFF); // test?

			short hulp = 0;
			short start = (short) (teller * 240);
			short end;
			if ((short) (((short) (teller + 1)) * 240) > (short) certServiceProvider.length) {
				end = (short) certServiceProvider.length;
			} else {
				end = (short) (start + 240);
			}

			for (short i = start; i < end; i++) {
				output[hulp] = certServiceProvider[i];
				hulp++;
			}

			apdu.setOutgoing();
			apdu.setOutgoingLength((short) output.length);
			apdu.sendBytesLong(output, (short) 0, (short) output.length);
		}
	}

	/**
	 * Identifies an available key slot. This does not mark the slot busy
	 * (allocation), it merely identifies it.
	 * 
	 * @return a key slot that is available, or -1 if all the slots are taken
	 */
	private static final byte findFreeKeySlot() {
		for (byte i = (byte) 0; i < keys.length; i++) {
			if (keys[i] == null)
				return i;
		}
		return INVALID_KEY;
	}

	private boolean checkIfPast(byte[] lastTime, byte[] tempTimeUpdate) {
		boolean past = true;
		for (short i = 0; i < 4; i++) {
			byte hulp = (byte) (lastTime[i] - tempTimeUpdate[i]);
			byte nul = 0x00;
			if (hulp != nul && hulp < nul) {
				past = false;
			}
		}
		return past;
	}

	public static final void random(byte[] buffer, short offset, short length) {
		randomizer.generateData(buffer, offset, length);
	}

	public boolean verifyPublic(RSAPublicKey pubKey, byte[] dataBuffer, byte[] signatureBuffer) {
		Signature signature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
		signature.init(pubKey, Signature.MODE_VERIFY);
		// signature.update(signatureBuffer, (short) 0, (short) 64);
		try {
			return signature.verify(dataBuffer, (short) 0, (short) 4, signatureBuffer, (short) 0, (short) 64);
		} catch (Exception e) {
			return false;
		}
	}

	public short generateSignature(RSAPrivateKey privKey, byte[] input, short offset, short length, byte[] output) {
		Signature signature = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
		signature.init(privKey, Signature.MODE_SIGN);
		short sigLength = signature.sign(input, offset, length, output, (short) 0);
		return sigLength;
	}

	private boolean compareTime() {
		// Twee tijden van elkaar aftrekken
		byte[] result = new byte[4];
		for (short i = 0; i < 4; i++) {
			result[i] = (byte) (tempTime[i] - lastTime[i]);
		}

		boolean refresh = false;
		// Vergelijken met threshold
		for (short i = 0; i < 4; i++) {
			if (i == 0) {
				if (tempTime[0] != lastTime[0]) {
					refresh = true;
					break;
				}
			} else if (result[i] != (short) threshold[i])
				if (result[i] < (short) threshold[i]) {
					refresh = true;
					break;
				} else {
					refresh = false;
					break;
				}
		}
		return refresh;
	}

	public byte[] slice(byte[] original, short offset, short end) {
		short length = (short) (end - offset);
		byte[] slice = new byte[length];

		for (short i = offset; i < end; i++) {
			short index = (short) (i - offset);
			slice[index] = original[i];
		}
		return slice;
	}

	// private void sendBytesEncryptedForMW(APDU apdu, byte[] data, short
	// dataLen) {
	// Cipher asymCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
	// asymCipher.init(pkMiddleware, Cipher.MODE_ENCRYPT);
	// byte[] encryptedData = new byte[256];
	//
	// byte[] dataToEncrypt = new byte[64];
	//
	// Util.arrayCopy(data, (short) 0, dataToEncrypt, (short) 0, (short)
	// data.length);
	//
	// asymCipher.doFinal(data, (short) 0, (short) data.length, encryptedData,
	// (short) 0);
	//
	// apdu.setOutgoing();
	// apdu.setOutgoingLength((short) encryptedData.length);
	// apdu.sendBytesLong(encryptedData, (short) 0, (short)
	// encryptedData.length);
	// }
	//
	// private byte[] receiveBytesEncryptedByMW(APDU apdu) {
	// byte[] buffer = apdu.getBuffer();
	// apdu.setIncomingAndReceive();
	// short inc = apdu.getIncomingLength();
	// byte[] encryptedData = new byte[inc];
	// Util.arrayCopy(buffer, (short) ISO7816.OFFSET_CDATA, encryptedData,
	// (short) 0, inc);
	//
	// Cipher asymCipher = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
	// asymCipher.init(secretKey, Cipher.MODE_DECRYPT);
	//
	// byte[] decryptedData = new byte[256];
	// short length = asymCipher.doFinal(encryptedData, (short) 0, (short) inc,
	// decryptedData, (short) 0);
	//
	// byte[] returnData = new byte[length];
	// Util.arrayCopy(decryptedData, (short) 0, returnData, (short) 0, length);
	//
	// return decryptedData;
	// }

	private byte[] cutOffNulls(byte[] data) {
		short length = (short) data.length;
		for (short i = length; i > 0; i--) {
			byte kappa = data[(short) (i - 1)];
			if (kappa != (byte) 0) {
				length = (short) (i);
				break;
			}
		}

		byte[] cleanedData = new byte[length];
		Util.arrayCopy(data, (short) 0, cleanedData, (short) 0, length);

		return cleanedData;
	}
}