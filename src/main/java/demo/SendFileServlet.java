package demo;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/send-file")
public class SendFileServlet extends HttpServlet {

    private static final Logger LOGGER =
            Logger.getLogger(SendFileServlet.class.getName());

    private static final String TARGET_URL =
            "https://daa-dev.development.creditguarantee.co.za/api/submissions";

    private static final String FILE_NAME = "452852-D2-20250929105010198.xlsx";
    private static final String FILE_NAME_HEADER = "452852-D2-20250929105010198";
    private static final String MIME_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        LOGGER.info("SendFileServlet: request received, starting upload process.");

        ServletContext ctx = getServletContext();
        try (InputStream fileStream =
                     ctx.getResourceAsStream("/WEB-INF/" + FILE_NAME)) {

            if (fileStream == null) {
                LOGGER.severe("SendFileServlet: file not found in /WEB-INF: " + FILE_NAME);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain");
                resp.getWriter().println("Could not find file in /WEB-INF: " + FILE_NAME);
                return;
            }

            LOGGER.info("SendFileServlet: reading file into memory: " + FILE_NAME);

            byte[] fileBytes = toByteArray(fileStream);

            LOGGER.info("SendFileServlet: file read complete, size = " + fileBytes.length + " bytes.");
            LOGGER.info("SendFileServlet: preparing HTTP POST to " + TARGET_URL);

            try (CloseableHttpClient client = HttpClients.createDefault()) {

                HttpPost httpPost = new HttpPost(TARGET_URL);

                httpPost.setHeader("fileName", FILE_NAME_HEADER);
                httpPost.setHeader("mimeType", MIME_TYPE);

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                builder.addBinaryBody(
                        "file",
                        fileBytes,
                        ContentType.create(MIME_TYPE),
                        FILE_NAME
                );

                builder.addTextBody("policyHolderId", "9991234567",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("reportingPeriodId", "3101",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("documentId", "af2683d0-18d0-43da-878b-54d5ee8c4bff",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("accountingPackageId", "1600",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("policyNumber", "452852",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));
                builder.addTextBody("userId", "CGEJA",
                        ContentType.TEXT_PLAIN.withCharset(StandardCharsets.UTF_8));

                HttpEntity multipart = builder.build();
                httpPost.setEntity(multipart);

                LOGGER.info("SendFileServlet: executing HTTP POST.");

                try (CloseableHttpResponse upstreamResponse = client.execute(httpPost)) {
                    int statusCode = upstreamResponse.getStatusLine().getStatusCode();
                    String body = upstreamResponse.getEntity() != null
                            ? EntityUtils.toString(upstreamResponse.getEntity(), StandardCharsets.UTF_8)
                            : "";

                    LOGGER.info("SendFileServlet: response received from API, status = " + statusCode);
                    if (statusCode >= 400) {
                        LOGGER.warning("SendFileServlet: API returned error status. Body: " + body);
                    } else {
                        LOGGER.fine("SendFileServlet: API response body: " + body);
                    }

                    resp.setContentType("text/plain; charset=UTF-8");
                    resp.getWriter().println("Request sent to: " + TARGET_URL);
                    resp.getWriter().println("Status from API: " + statusCode);
                    resp.getWriter().println("Response body:");
                    resp.getWriter().println(body);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "SendFileServlet: exception while calling API", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain; charset=UTF-8");
                resp.getWriter().println("Error while sending request: " + e.getMessage());
            }
        }

        LOGGER.info("SendFileServlet: upload process finished.");
    }

    private static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int read;
        while ((read = in.read(tmp)) != -1) {
            buffer.write(tmp, 0, read);
        }
        return buffer.toByteArray();
    }
}
