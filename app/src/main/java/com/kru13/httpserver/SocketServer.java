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
			ClientHandler client = new ClientHandler(serverSocket);
			client.run();
			//client.join();

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
