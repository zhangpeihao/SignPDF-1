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

            // carl:允许改名,没有就用报表名
            String fileName = WebUtils.getHTTPRequestParameter(req, ParameterConsts.__FILENAME__);

            //barry:op=fs程序报表用到
            if(fileName == null){
                fileName = (String)sessionIDInfor.getParameterValue(ParameterConsts.__FILENAME__);
            }
            if (fileName == null) {
                //p:需要先获得导出报表的名字.
                fileName = sessionIDInfor.getWebTitle().replaceAll("\\s", "_");
                //shoc 导出文件名不能含有逗号 否则抛错
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

                // 标志是否正在为客户端PDF打印而生成PDF文件
                sessionIDInfor.setAttribute(SessionIDInforAttribute.GENPDFPRINT, Boolean.FALSE);
            }

        }


    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    /**
     * 处理应答
     * @param res http应答
     */
    public static void dealResponse4Export(HttpServletResponse res) {
        //marks:ie6不是嵌在网页中情况下，点保存正常，点打开就出现问题！
        //marks:要对cache进行配置和给其权限，支持https
        // carl:设置不用缓存，生存周期设个3秒，不然会导致bug0004207.
        res.setHeader("Cache-Control", "public");
        res.setHeader("Cache-Control", "max-age=3");
        // 需要首先reset一下,保证buffer里面没有其他东西.
        // 在Weblogic里面常常没有清空buffer里面的东西.
        res.reset();
    }
}
