package com.kru13.httpserver;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

public class ClientHandler extends Thread {

    public static String NEWLINE = "\r\n";
    private ServerSocket serverSocket;
    private Handler messageHandler;
    private boolean bRunning = false;

    public ClientHandler(ServerSocket socket, Handler h)
    {
        this.serverSocket = socket;
        this.messageHandler = h;
    }

    public void run() {

        try {

            bRunning = true;
            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept();
                (new ClientHandler(serverSocket,messageHandler)).start();
                Log.d("SERVER", "Socket Accepted");

                OutputStream o = s.getOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                ArrayList<String> responses = new ArrayList<String>();
                String response;
                while(!(response = in.readLine()).isEmpty())
                {
                    responses.add(response);
                }
                HttpRequest request = HttpRequest.ParseRequest(responses);
                if(request != null)
                {
                    ResponseMessage message = new ResponseMessage();
                    if (request.Method.toUpperCase().equals("GET"))
                    {
                        File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),request.FileName);
                        if (outFile.exists())
                        {
                            message.ResponseSize = outFile.length();
                            message.FileName = request.FileName;
                            message.Host = "localhost/12345";
                            message.ResponseType = "200 OK";

                            String res = CreateHeaderString(request,String.valueOf(message.ResponseSize),message.ResponseType,message.Host,"Closed");
                            message.ResponseSize += res.length();
                            out.write(res);
                            out.write(NEWLINE);
                            out.flush();
                            byte[] buf = new byte[1024];
                            int len = 0;
                            FileInputStream fis = new FileInputStream(outFile);
                            while((len = fis.read(buf)) > 0)
                            {
                                o.write(buf,0,len);
                            }
                        }
                        else
                        {
                            File notFoundFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"notFound.html");
                            message.ResponseSize = notFoundFile.length();
                            message.FileName = "notFound.html";
                            message.Host = "localhost/12345";
                            message.ResponseType = "404 Not Found";

                            String res = CreateHeaderString(request,String.valueOf(message.ResponseSize),message.ResponseType,message.Host,"Closed");
                            message.ResponseSize += res.length();
                            out.write(res);
                            out.write(NEWLINE);
                            out.flush();

                            byte[] buf = new byte[1024];
                            int len = 0;
                            FileInputStream fis = new FileInputStream(notFoundFile);
                            while((len = fis.read(buf)) > 0)
                            {
                                o.write(buf,0,len);
                            }

                            Log.d("SERVER","File not found");
                        }
                    }
                    else if(request.Method.toUpperCase().equals("PUT"))
                    {
                        Log.d("SERVER","Put methode");

                    }
                    else
                    {
                        Log.d("SERVER","bad request method!");
                    }

                    Message msg = messageHandler.obtainMessage();
                    Bundle bndl = new Bundle();
                    bndl.putSerializable("REQUEST",(Serializable)message);
                    msg.setData(bndl);
                    messageHandler.sendMessage(msg);
                }
                else
                {

                }

                s.close();
                Log.d("SERVER", "Socket Closed");
            }
        }
        catch (Exception e)
        {
            Log.d("SERVER",e.toString());
        }
        finally {
            serverSocket = null;
            bRunning = false;
        }
    }

    private String CreateHeaderString(HttpRequest request, String size,String responseType,String server,String conn)
    {
        String res = request.HttpVersion +" " + responseType + " " + NEWLINE;
        res += "Date: "+Calendar.getInstance().getTime()+ NEWLINE;
        res += "Server: "+ server + NEWLINE;
        res +="Content-Length: " + size + NEWLINE;
        res +="Connection: "+ conn + NEWLINE;
        res +=NEWLINE;
        return res;
    }
}
