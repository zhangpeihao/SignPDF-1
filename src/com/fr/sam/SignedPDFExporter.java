package com.fr.sam;

import com.fr.io.attr.PDFExportAttr;
import com.fr.io.attr.ReportExportAttr;
import com.fr.io.exporter.PDFExporter;
import com.fr.page.PagePainterProvider;
import com.fr.page.PageSetProvider;
import com.fr.page.ReportPageProvider;
import com.fr.performance.PerformanceManager;
import com.fr.performance.status.ReportStatus;
import com.fr.stable.Constants;
import com.fr.stable.StringUtils;
import com.fr.stable.bridge.StableFactory;
import com.fr.third.com.lowagie.text.Document;
import com.fr.third.com.lowagie.text.DocumentException;
import com.fr.third.com.lowagie.text.Rectangle;
import com.fr.third.com.lowagie.text.pdf.PdfContentByte;
import com.fr.third.com.lowagie.text.pdf.PdfReader;
import com.fr.third.com.lowagie.text.pdf.PdfStamper;
import com.fr.third.com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.crypto.CryptoException;
import srvSeal.Base64;
import srvSeal.PdfAutoSeal;
import srvSeal.SignUtil;

import java.awt.*;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;

/**
 * author: sam
 * date: 15/6/17
 */
public class SignedPDFExporter extends PDFExporter {
    final static int BUFFER_SIZE = 4096;

    private String reportName;

    public SignedPDFExporter() {
        super();
    }

    public SignedPDFExporter(boolean b) {
        super(b);
    }

    public SignedPDFExporter(boolean isPrint, String fileName) {
        this.isPrint = isPrint;
        this.reportName = fileName;
    }

    public void export(OutputStream out, PageSetProvider pageSet, ReportExportAttr reportExportAttr) throws Exception {
        //Document.
        PerformanceManager.getRuntimeMonitor().setCurrentSessionStatus(ReportStatus.EXPORT_PDF);
        String password = null;
        PdfWriter writer = null;
        ByteArrayOutputStream bo = new ByteArrayOutputStream();

        PDFExportAttr pwd = reportExportAttr == null ? null : reportExportAttr.getPDFExportAttr();
        if (pwd != null && StringUtils.isNotEmpty(pwd.getPassword())) {
            password = pwd.getPassword();
        }

        boolean isFirstPage = true;
        long lastSize = 0;
        Document document = getDocument(pageSet, writer, bo, isFirstPage, lastSize);

        if (document != null) {
            document.close();
        }

        if (password != null) {
            PdfReader pdfReader = new PdfReader(bo.toByteArray());
            pdfReader.removeUsageRights();
            PdfStamper stamper = new PdfStamper(pdfReader, out);
            // neil:��������, ��߿��Խ��и���ϸ�Ŀ��ƣ��Ƿ���Դ�ӡ���Ƿ���Ը��Ƶ�Ȩ��
            stamper.setEncryption(false, password, "", PdfWriter.ALLOW_ASSEMBLY
                    | PdfWriter.ALLOW_DEGRADED_PRINTING | PdfWriter.ALLOW_FILL_IN | PdfWriter.ALLOW_MODIFY_ANNOTATIONS
                    | PdfWriter.ALLOW_MODIFY_CONTENTS | PdfWriter.ALLOW_PRINTING | PdfWriter.ALLOW_SCREENREADERS | PdfWriter.ALLOW_COPY);
            stamper.close();
        }

        try {
            byte[] resultPDF = null;
            resultPDF = getSignedPDF(bo.toByteArray());
            out.write(resultPDF);
            out.flush();
            out.close();
        } catch (IOException e) {
            catchException();
        }
        PerformanceManager.getRuntimeMonitor().setCurrentSessionStatus(ReportStatus.COMPLETE);
    }

    public byte[] getSignedPDF(byte[] oriPDF) {
        byte[] resultPDF = null;
        try {
            //���ù̶�·������
            PdfAutoSeal pdfAutoSeal = new PdfAutoSeal();
            String imagePath = "C:\\PDF\\obm.bmp";
            String imageBase64 = getImageBase64(imagePath);
            String text = "123213";
            String ruleInfo = "AUTO_ADD:0,-1,0,0,255," + text + ")|(0,";
//            String cerPath = getCerPathByReportName(reportName);//���ݱ�������̬ѡ��֤��
            String cerPath= "C:\\PDF\\��װ����1.pfx";
            String cerPwd = "1";

            KeyStore store = buildKeyStore(cerPath, cerPwd.toCharArray());

            //signBase64�������ĵ����ݵ�base64;�ĵ�hashbase64;pos����
            String signBase64 = pdfAutoSeal.addSealImage(oriPDF, imageBase64, ruleInfo);
            byte[] signedData = null;
            signedData = SignUtil.signP7Bytes(Base64.decode(signBase64.split(";")[1]), store, cerPwd);

            resultPDF = pdfAutoSeal.savePdf(signBase64.split(";")[0], Integer.parseInt(signBase64.split(";")[2]), signedData);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return resultPDF;
    }

    public String getCerPathByReportName (String reportName) {
        String cerPath = "";
        return cerPath;
    }

    public  KeyStore buildKeyStore(String filename, char[] password) throws CryptoException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        FileInputStream fis = new FileInputStream(filename);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(fis, password);
        fis.close();
        return keyStore;
    }

    public String getImageBase64(String imagePath) {
        String result = "";
        byte[] data = null;
        File file = new File(imagePath);
        try {
            data = FileUtils.readFileToByteArray(file);
            result = Base64.encodeToString(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void catchException() {

    }

    private Document getDocument(PageSetProvider pageSet, PdfWriter writer, ByteArrayOutputStream bo, boolean isFirstPage, long lastSize) throws DocumentException {
        Document document = null;
        for (int index = 0; index < pageSet.size(); index++) {
            long currentSize = bo.size();
            PerformanceManager.getRuntimeMonitor().addMemoryAndCheck(currentSize - lastSize);
            lastSize = currentSize;
            ReportPageProvider reportPage = pageSet.getPage(index);
            if (reportPage == null){ break;}
            // james��PDF�����ڴ�ӡ�ģ���ӡ��Ĭ�Ϸֱ���Ϊ72PPI������������������
            float reportWidth = reportPage.getPaperWidth().toPixF(Constants.DEFAULT_PRINT_AND_EXPORT_RESOLUTION);
            float reportHeight = reportPage.getPaperHeight().toPixF(Constants.DEFAULT_PRINT_AND_EXPORT_RESOLUTION);

            if (document == null) {
                document = new Document(new Rectangle(reportWidth, reportHeight));
                writer = PdfWriter.getInstance(document, bo);
                openDocument(document);

            }

            //james��Ҫ֧�ֲ�ͬ��ҳ�����ã���һ��Page����ҪnewPage������������ҪnewPage
            if (!isFirstPage) {
                document.setPageSize(new Rectangle(reportWidth, reportHeight));
                document.newPage();
            }
            isFirstPage = false;

            //��ReportPage����PDF��ȥ
            PdfContentByte cb = writer.getDirectContent();
            Graphics2D g2d = cb.createGraphics(reportWidth, reportHeight, prepareFontMapper());

            // carl:����ԭ��Ĭ�ϵĻ���dpi��96����72������»�����Щ�������ɲ���ȷ���ֲڵ����⡣so����96�»���
            double scale = Constants.DEFAULT_PRINT_AND_EXPORT_RESOLUTION * 1d / Constants.FR_PAINT_RESOLUTION;
            g2d.scale(scale, scale);
            HashMap pagePainterClass = new HashMap();
            pagePainterClass.put(Constants.ARG_0, ReportPageProvider.class);
            pagePainterClass.put(Constants.ARG_1, Graphics2D.class);
            PagePainterProvider bp = (PagePainterProvider) StableFactory.createNewObject(PagePainterProvider.XML_TAG,
                    new Object[]{reportPage, g2d,
                            Integer.valueOf(String.valueOf(Constants.FR_PAINT_RESOLUTION)), Boolean.valueOf(isPrint)}, pagePainterClass);
            bp.convert();

            g2d.dispose();
        }
        return document;
    }

    private void openDocument (Document document) {
        document.open();
    }

    // ��InputStreamת����byte����
    public byte[] InputStreamTOByte(InputStream in) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = -1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
            outStream.write(data, 0, count);
        data = null;

        return outStream.toByteArray();
    }

    // ��byte����ת����InputStream
    public InputStream byteTOInputStream(byte[] in) throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream(in);
        return is;
    }
}
