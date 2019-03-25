package com.kru13.httpserver;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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
    private HttpServerActivity activity;
    private boolean bRunning = false;

    private ByteArrayOutputStream imageBuffer;
    public static DataOutputStream stream;
    private String boundary = "--boundary";
    private boolean closeSocket = true;


    public ClientHandler(ServerSocket socket, Handler h,HttpServerActivity act)
    {
        this.serverSocket = socket;
        this.messageHandler = h;
        this.activity = act;


        imageBuffer = new ByteArrayOutputStream();
    }

    public void run() {

        try {

            bRunning = true;
            while (bRunning) {
                Log.d("SERVER", "Socket Waiting for connection");

                Socket s = serverSocket.accept();

                (new ClientHandler(serverSocket,messageHandler,activity)).start();
                Log.d("SERVER", "Socket Accepted");

                OutputStream o = s.getOutputStream();
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
                BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

                ArrayList<String> responses = new ArrayList<String>();
                String response;
                String firstLine = "";
                while((response = in.readLine()) != null)
                {
                    if(firstLine.isEmpty())
                    {
                        firstLine = response;
                    }
                    else if (firstLine.contains("GET") && response.isEmpty())
                    {
                        break;
                    }
                    responses.add(response);
                }
                HttpRequest request = HttpRequest.ParseRequest(responses);
                if(request != null)
                {
                    ResponseMessage message = new ResponseMessage();
                    if (request.Method.toUpperCase().equals("GET"))
                    {
                        if (request.FileName.contains("android"))
                        {
                            message.ResponseSize = activity.GetPicture().length;
                            message.FileName = request.FileName;
                            message.Host = request.Host;
                            message.ResponseType = "200 OK";

                            out.write(CreateHeaderForImage(request,String.valueOf(message.ResponseSize),message.ResponseType));
                            out.flush();

                            o.write(activity.GetPicture(),0,activity.GetPicture().length);
                        }
                        else if (request.FileName.contains("stream"))
                        {
                            stream = new DataOutputStream(s.getOutputStream());
                            if (stream != null)
                            {
                                try
                                {
                                    Log.d("onPreviewFrame", "stream succ");
                                    stream.write(CreateHeaderForStream(request,message.ResponseType,"localhost/12345"));

                                    stream.flush();

                                    closeSocket = false;
                                    Log.d("onPreviewFrame", "stream created");

                                    sendStreamData();
                                }
                                catch (IOException e)
                                {
                                    Log.d("ERROR:", e.getLocalizedMessage());
                                }
                            }
                        }
                        else if (request.FileName.contains("cgi-bin"))
                        {
                            try{
                                Process process = Runtime.getRuntime().exec(request.Command);
                                BufferedReader bufferedReader = new BufferedReader(
                                        new InputStreamReader(process.getInputStream()));

                                String res = CreateCmdFileText(request,message.ResponseType);
                                String line;
                                while ((line = bufferedReader.readLine()) != null){
                                    res +="<pre>" + line + "</pre>";
                                }
                                res += NEWLINE;
                                res +="</html>";

                                out.write(res);
                                out.flush();

                            }
                            catch (Exception e){
                                Log.d("ProcessOutput", "just failed: " + e.getMessage());

                            }
                        }
                        else
                        {
                            File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),request.FileName);
                            if (outFile.exists())
                            {
                                message.ResponseSize = outFile.length();
                                message.FileName = request.FileName;
                                message.Host = "localhost/12345";
                                message.ResponseType = "200 OK";

                                String res = CreateHeaderString(request,String.valueOf(message.ResponseSize),message.ResponseType,"localhost/12345","Closed");
                                message.ResponseSize += res.length();
                                out.write(res);
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

                                String res = CreateHeaderString(request,String.valueOf(message.ResponseSize),message.ResponseType,"localhost/12345","Closed");
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

                    }
                    else if(request.Method.toUpperCase().equals("POST"))
                    {
                        if (request.Path.contains("upload"))
                        {
                            File data = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"uploadedData.txt");
                            BufferedWriter writer = new BufferedWriter(new FileWriter(data)); // When I debug, it comes up until here and then returns with the exception
                            writer.write(request.ContentData);
                            writer.close();
                        }

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

                if(closeSocket){
                    s.close();

                    Log.d("SERVER", "Socket Closed");
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
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
        //res +="Connection: "+ conn + NEWLINE;
        res +=NEWLINE;
        return res;
    }

    private byte[] CreateHeaderForStream(HttpRequest request,String responseType,String server )
    {
        byte[] res = (request.HttpVersion +" " + responseType + " " + NEWLINE +
                "Server: "+ server + NEWLINE +
                "Cache-Control:  no-cache" + NEWLINE +
                "Cache-Control:  private" + NEWLINE +
                "Content-Type: multipart/x-mixed-replace;boundary=" + boundary + NEWLINE ).getBytes();
        return res;
    }

    private String CreateHeaderForImage(HttpRequest request,String size,String responseType)
    {
        String res = request.HttpVersion +" " + responseType + " " + NEWLINE;
        res += "Date: " + Calendar.getInstance().getTime() + NEWLINE;
        res +="Content-Length: " + size + NEWLINE;
        res +="Content-Type: image/jpeg" + NEWLINE;
        res += NEWLINE;
        return res;
    }

    private String CreateCmdFileText(HttpRequest request,String responseType)
    {
        String res = request.HttpVersion +" " + responseType + " " + NEWLINE;
        res += "Date: " + Calendar.getInstance().getTime() + NEWLINE;
        res +="Content-Type: text/html" + NEWLINE;
        res +=NEWLINE;
        res +="<html>";
        return res;
    }

    private void sendStreamData(){
        if (stream != null){
            try
            {
                byte[] baos = activity.GetPicture();
                // buffer is a ByteArrayOutputStream
                imageBuffer.reset();
                imageBuffer.write(baos);
                imageBuffer.flush();
                // write the content header
                stream.write((NEWLINE +  boundary + NEWLINE +
                        "Content-type: image/jpeg" + NEWLINE +
                        "Content-Length: " + imageBuffer.size() + NEWLINE + NEWLINE).getBytes());

                stream.write(imageBuffer.toByteArray());
                stream.write((NEWLINE ).getBytes());

                stream.flush();
                Log.d("onPreviewFrame", "succ");
            }
            catch (IOException e)
            {
                Log.d("onPreviewFrame error:  ", e.getLocalizedMessage());
            }
        }
        else{

            Log.d("onPreviewFrame", "null");
        }

        messageHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendStreamData();
            }
        }, 100);
    }

}
