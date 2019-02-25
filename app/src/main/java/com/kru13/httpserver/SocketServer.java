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

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

public class SocketServer extends Thread {

    public SocketServer(Handler h)
    {
        this.messageHandler = h;
    }

	ServerSocket serverSocket;
    Handler messageHandler;
	private final int port = 12345;

	public void close() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			Log.d("SERVER", "Error, probably interrupted in accept(), see log");
			e.printStackTrace();
		}
	}
	
	public void run() {

		Log.d("SERVER", "Creating Socket");
		try {
			serverSocket = new ServerSocket(port);
			ClientHandler client = new ClientHandler(serverSocket, messageHandler);
			client.run();


		} catch (IOException e) {
			if (serverSocket != null && serverSocket.isClosed())
				Log.d("SERVER", "Normal exit");
			else {
				Log.d("SERVER", "Error");
				e.printStackTrace();
			}
		}

	}

}
