package com.kru13.httpserver;

import java.io.Serializable;
import java.util.ArrayList;

public class HttpRequest implements Serializable
{
    public String Method;
    public String Path;
    public String FileName;
    public String HttpVersion;
    public String Host;
    public String Connection;
    public String Command;
    public String Data;

    public String Boundary = "";
    public String ContentType;
    public String UploadedFileName;
    public String ContentData = "";


    public static HttpRequest ParseRequest(ArrayList<String> responses)
    {
        if (responses.isEmpty())
            return null;

        boolean readContentHead = false;
        boolean readContentData = false;
        HttpRequest request = new HttpRequest();
        for (String res:responses) {

            if(res.contains("GET"))
            {
                String header[] = res.split(" ");
                request.Method = header[0];
                request.Path = header[1];
                request.HttpVersion = header[2];
                request.FileName = header[1].substring(header[1].indexOf('/')+1);
                request.Host = res;
                if (header[1].indexOf("/cgi-bin") != -1)
                {
                    request.Command = header[1].substring(header[1].indexOf("/cgi-bin")+9);
                }
                else
                    request.Command = "";

            }
            else if (res.contains("POST"))
            {
                String header[] = res.split(" ");
                request.Method = header[0];
                request.Path = header[1];
                request.HttpVersion = header[2];
                request.FileName = header[1].substring(header[1].indexOf('/')+1);
            }
            else if (res.contains("boundary"))
            {
                request.Boundary = res.substring(res.indexOf("boundary")+9);
            }
            else if (!request.Boundary.isEmpty() && res.contains(request.Boundary))
            {
                if (!readContentHead)
                    readContentHead = true;
                else
                {
                    readContentHead = false;
                    readContentData = false;
                }
            }
            else if (readContentHead && res.contains("filename"))
            {
                request.UploadedFileName = res.substring(res.indexOf("filename") + 9);
            }
            else if (readContentHead && res.contains("Content-Type"))
            {
                request.ContentType = res.substring(res.indexOf("Content-Type") + 13);
            }
            else if (readContentHead && res.isEmpty())
            {
                readContentData = true;
            }
            else if (readContentData)
            {
                request.ContentData += res;
            }

        }

        return request;
    }
}
