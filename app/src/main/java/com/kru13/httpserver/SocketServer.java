package com.kru13.httpserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;

public class SocketServer extends Thread {
	
	ServerSocket serverSocket;
	public final int port = 12345;
	boolean bRunning;
	
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
		bRunning = false;
	}
	
	public void run() {
        try {
        	Log.d("SERVER", "Creating Socket");
            serverSocket = new ServerSocket(port);
            bRunning = true;
            while (bRunning) {
            	Log.d("SERVER", "Socket Waiting for connection");
                Socket s = serverSocket.accept(); 
                Log.d("SERVER", "Socket Accepted");
                
                OutputStream o = s.getOutputStream();
	        	BufferedWriter out = new BufferedWriter(new OutputStreamWriter(o));
	        	BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));

				ArrayList<String> responses = new ArrayList<String>();
	        	String response;
	            while(!(response = in.readLine()).isEmpty())
				{
					responses.add(response);
					Log.d("SERVER",response);
				}
				if(!responses.isEmpty())
				{
					String header[] = responses.get(0).split(" ");
					if (header[0].toUpperCase().equals("GET"))
					{
						String fileName = header[1].substring(header[1].lastIndexOf("/")+1);
						File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),fileName);
						//TODO nemam prava na ten subor
						if (outFile.exists())
						{
							BufferedReader outFileStream = new BufferedReader(new InputStreamReader(new FileInputStream(outFile)));
							StringBuilder strBuilder = new StringBuilder();
							int fileLength = 0;
							while(!(response = outFileStream.readLine()).isEmpty())
							{
								response = response + "\n";
								fileLength += response.length();
								strBuilder.append(response);
							}
							outFileStream.close();
							out.write(header[2] + " 200 OK");
							out.write("Date: "+Calendar.getInstance().getTime());
							out.write("Server: localhost/12345");
							out.write("Content-Length: " + fileLength);
							out.write("Connection: Closed");
							out.write("Content-Type: text/html");
							out.write("");
							out.write(strBuilder.toString());
							out.flush();
						}
						else
						{
							File notFoundFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"notFound.html");
							BufferedReader outFileStream = new BufferedReader(new InputStreamReader(new FileInputStream(notFoundFile)));
							StringBuilder strBuilder = new StringBuilder();
							int fileLength = 0;
							while(!(response = outFileStream.readLine()).isEmpty())
							{
								response = response + "\n";
								fileLength += response.length();
								strBuilder.append(response);
							}
							outFileStream.close();
							out.write(header[2] + " 404 Not Found");
							out.write("Date: "+Calendar.getInstance().getTime());
							out.write("Server: localhost/12345");
							out.write("Content-Length: " + fileLength);
							out.write("Connection: Closed");
							out.write("Content-Type: text/html");
							out.write("");
							out.write(strBuilder.toString());
							out.flush();
							Log.d("SERVER","File not found");
						}
					}
					else if(header[0].toUpperCase().equals("PUT"))
					{
						Log.d("SERVER","Put methode");

					}
					else
					{
						Log.d("SERVER","bad request methode!");
					}

				}
				else
				{

				}
	            
                s.close();
                Log.d("SERVER", "Socket Closed");
            }
        } 
        catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed())
            	Log.d("SERVER", "Normal exit");
            else {
            	Log.d("SERVER", "Error");
            	e.printStackTrace();
            }
        }
        finally {
        	serverSocket = null;
        	bRunning = false;
        }
    }

}
