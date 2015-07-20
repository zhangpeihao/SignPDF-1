package com.fr.sam;

import com.fr.general.ComparatorUtils;
import com.fr.general.web.ParameterConsts;
import com.fr.web.Browser;
import com.fr.web.SessionIDInforAttribute;
import com.fr.web.core.ReportSessionIDInfor;
import com.fr.web.core.SessionDealWith;
import com.fr.web.core.utils.ExportUtils;
import com.fr.web.utils.WebUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * author: sam
 * date: 15/6/17
 */
public class SignedPDFExporterService extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sessionID = WebUtils.getHTTPRequestParameter(req, "sessionID");
        if (sessionID != null) {
            ReportSessionIDInfor sessionIDInfor = (ReportSessionIDInfor) SessionDealWith.getSessionIDInfor(sessionID);
            dealResponse4Export(resp);

            String format = WebUtils.getHTTPRequestParameter(req, "format");

            // carl:�������,û�о��ñ�����
            String fileName = WebUtils.getHTTPRequestParameter(req, ParameterConsts.__FILENAME__);

            //barry:op=fs���򱨱��õ�
            if(fileName == null){
                fileName = (String)sessionIDInfor.getParameterValue(ParameterConsts.__FILENAME__);
            }
            if (fileName == null) {
                //p:��Ҫ�Ȼ�õ������������.
                fileName = sessionIDInfor.getWebTitle().replaceAll("\\s", "_");
                //shoc �����ļ������ܺ��ж��� �����״�
                fileName = fileName.replaceAll(",", "_");
            }

            Browser browser = Browser.resolve(req);
            fileName = browser.getEncodedFileName4Download(fileName);
            boolean isPDFPrint = ComparatorUtils.equals(WebUtils.getHTTPRequestParameter(req, "isPDFPrint"), "true");

            OutputStream outputStream = resp.getOutputStream();

            if (format.equalsIgnoreCase("PDF")) {
                ExportUtils.setPDFContent(resp, fileName, false);
                SignedPDFExporter signedPDFExporter = new SignedPDFExporter(isPDFPrint, fileName);

                try {
                    signedPDFExporter.export(outputStream, sessionIDInfor.getWorkBook2Show());

                    outputStream.flush();
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // ��־�Ƿ�����Ϊ�ͻ���PDF��ӡ������PDF�ļ�
                sessionIDInfor.setAttribute(SessionIDInforAttribute.GENPDFPRINT, Boolean.FALSE);
            }

        }


    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    /**
     * ����Ӧ��
     * @param res httpӦ��
     */
    public static void dealResponse4Export(HttpServletResponse res) {
        //marks:ie6����Ƕ����ҳ������£��㱣����������򿪾ͳ������⣡
        //marks:Ҫ��cache�������ú͸���Ȩ�ޣ�֧��https
        // carl:���ò��û��棬�����������3�룬��Ȼ�ᵼ��bug0004207.
        res.setHeader("Cache-Control", "public");
        res.setHeader("Cache-Control", "max-age=3");
        // ��Ҫ����resetһ��,��֤buffer����û����������.
        // ��Weblogic���泣��û�����buffer����Ķ���.
        res.reset();
    }
}
