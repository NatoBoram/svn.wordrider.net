package cz.vity.freerapid.plugins.services.mega;

import cz.vity.freerapid.plugins.exceptions.NotRecoverableDownloadException;
import cz.vity.freerapid.plugins.exceptions.PluginImplementationException;
import cz.vity.freerapid.plugins.exceptions.ServiceConnectionProblemException;
import cz.vity.freerapid.plugins.exceptions.URLNotAvailableAnymoreException;
import cz.vity.freerapid.plugins.webclient.DownloadClient;
import cz.vity.freerapid.plugins.webclient.MethodBuilder;
import cz.vity.freerapid.plugins.webclient.interfaces.HttpDownloadClient;
import cz.vity.freerapid.plugins.webclient.utils.PlugUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * @author ntoskrnl
 */
class MegaApi {

    private static final Logger logger = Logger.getLogger(MegaApi.class.getName());

    private final HttpDownloadClient client;
    private final byte[] key;
    private byte[] nonce;
    private int seqno = new Random().nextInt(0x10000000);

    public MegaApi(final HttpDownloadClient client, final String key) throws Exception {
        this.client = new DownloadClient();
        this.client.initClient(client.getSettings());
        this.key = prepareKey(key);
    }

    private byte[] prepareKey(final String key) throws Exception {
        final byte[] b = Base64.decodeBase64(key);
        final int L = b.length / 2;
        final byte[] result = new byte[L];
        for (int i = 0; i < L; i++) {
            result[i] = (byte) (b[i] ^ b[L + i]);
        }
        nonce = Arrays.copyOfRange(b, L, L + 8);
        return result;
    }

    public String request(final String content, final String folderId) throws Exception {
        String params = "";
        if (folderId != null) {
            params += "&n=" + folderId;
        }
        final HttpMethod method = new MethodBuilder(client).setAction("https://g.api.mega.co.nz/cs?id=" + seqno++ + params).toPostMethod();
        ((PostMethod) method).setRequestEntity(new StringRequestEntity(content, "text/plain", "UTF-8"));
        if (client.makeRequest(method, true) != HttpStatus.SC_OK) {
            throw new ServiceConnectionProblemException();
        }
        checkProblems(client.getContentAsString());
        final Matcher matcher = PlugUtils.matcher("\"at\"\\s*:\\s*\"(.+?)\"", client.getContentAsString());
        if (!matcher.find()) {
            logger.warning("Content from API request:\n" + client.getContentAsString());
            throw new PluginImplementationException("Error parsing server response");
        }
        return client.getContentAsString() + "\n" + decryptData(matcher.group(1));
    }

    private String decryptData(final String data) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(new byte[16]));
        return new String(cipher.doFinal(Base64.decodeBase64(data)), "UTF-8");
    }

    public Cipher getDownloadCipher(final long startPosition) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        final ByteBuffer buffer = ByteBuffer.allocate(16).put(nonce);
        buffer.asLongBuffer().put(startPosition / 16);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(buffer.array()));
        final int skip = (int) (startPosition % 16);
        if (skip != 0) {
            if (cipher.update(new byte[skip]).length != skip) {
                //that should always work with a CTR mode cipher
                throw new IOException("Failed to skip bytes from cipher");
            }
        }
        return cipher;
    }

    public static void checkProblems(final String content) throws Exception {
        Matcher matcher = PlugUtils.matcher("^(\\-\\d+?)$", content);
        if (!matcher.find()) {
            matcher = PlugUtils.matcher("\\[(\\-\\d+?)\\]", content);
            if (!matcher.find()) {
                matcher = PlugUtils.matcher("\"e\":(\\-\\d+)", content);
                if (!matcher.find()) {
                    return;
                }
            }
        }
        final int e = Integer.parseInt(matcher.group(1));
        switch (e) {
            case ETOOMANYCONNECTIONS:
                throw new ServiceConnectionProblemException("Too many connections for this download");
            case ENOENT:
            case EBLOCKED:
            case ETOOMANY:
            case EACCESS:
                throw new URLNotAvailableAnymoreException("File not found");
            case EOVERQUOTA:
            case EAGAIN:
            case ETEMPUNAVAIL:
                throw new ServiceConnectionProblemException("Temporary error");
            default:
                throw new NotRecoverableDownloadException("Unknown server error (" + e + ")");
        }
    }

    private static final int EINTERNAL = -1;
    private static final int EARGS = -2;
    private static final int EAGAIN = -3;
    private static final int ERATELIMIT = -4;
    private static final int EFAILED = -5;
    private static final int ETOOMANY = -6;
    private static final int ERANGE = -7;
    private static final int EEXPIRED = -8;
    private static final int ENOENT = -9;
    private static final int ECIRCULAR = -10;
    private static final int EACCESS = -11;
    private static final int EEXIST = -12;
    private static final int EINCOMPLETE = -13;
    private static final int EKEY = -14;
    private static final int ESID = -15;
    private static final int EBLOCKED = -16;
    private static final int EOVERQUOTA = -17;
    private static final int ETEMPUNAVAIL = -18;
    private static final int ETOOMANYCONNECTIONS = -19;
    private static final int EWRITE = -20;
    private static final int EREAD = -21;
    private static final int EAPPKEY = -22;

}
