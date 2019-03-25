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


    public static HttpRequest ParseRequest(ArrayList<String> responses)
    {
        if (responses.isEmpty())
            return null;

        HttpRequest request = new HttpRequest();
        for (String res:responses) {

            if(res.contains("GET") || res.contains("POST"))
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
        }

        return request;
    }
}
