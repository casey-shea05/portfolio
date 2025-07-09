/*
 * RecordsDatabaseService.java
 *
 * The service threads for the records database server.
 * This class implements the database access service, i.e. opens a JDBC connection
 * to the database, makes and retrieves the query, and sends back the result.
 *
 * author: 2591154
 *
 */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
//import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.Socket;

import java.util.StringTokenizer;

import java.sql.*;
import javax.sql.rowset.*;
    //Direct import of the classes CachedRowSet and CachedRowSetImpl will fail becuase
    //these clasess are not exported by the module. Instead, one needs to impor
    //javax.sql.rowset.* as above.



public class RecordsDatabaseService extends Thread{

    private Socket serviceSocket = null;
    private String[] requestStr  = new String[2]; //One slot for artist's name and one for recordshop's name.
    private ResultSet outcome   = null;

	//JDBC connection
    private String USERNAME = Credentials.USERNAME;
    private String PASSWORD = Credentials.PASSWORD;
    private String URL      = Credentials.URL;



    //Class constructor
    public RecordsDatabaseService(Socket aSocket){
        
		//TO BE COMPLETED
		serviceSocket = aSocket;
        this.start();
    }


    //Retrieve the request from the socket
    public String[] retrieveRequest()
    {
        this.requestStr[0] = ""; //For artist
        this.requestStr[1] = ""; //For recordshop
		
		String tmp = "";
        try {

			//TO BE COMPLETED
			InputStream socketstream = this.serviceSocket.getInputStream();
            InputStreamReader socketReader = new InputStreamReader(socketstream);
            StringBuffer stringBuffer = new StringBuffer();
            char x;

            while(true) {
                x = (char) socketReader.read();
                if (x == '#') {
                    break;
                }
                stringBuffer.append(x);
                tmp = stringBuffer.toString();
            }

            String[] parts = tmp.split(";");

            if (parts.length == 2){
                this.requestStr[0] = parts[0];
                this.requestStr[1] = parts[1];
            } else {
                this.requestStr[0] = "Invalid";
                this.requestStr[1] = "Invalid";
                System.out.println("Invalid message format: " + tmp);
            }


         }catch(IOException e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        return this.requestStr;
    }


    //Parse the request command and execute the query
    public boolean attendRequest()
    {
        boolean flagRequestAttended = true;
		
		this.outcome = null;
		
		String sql = "SELECT\n" +
                "    record.title,\n" +
                "    record.label,\n" +
                "    record.genre,\n" +
                "    record.rrp,\n" +
                "    COUNT(recordcopy.copyID) AS copies_available\n" +
                "FROM\n" +
                "    record\n" +
                "JOIN\n" +
                "    recordcopy ON record.recordID = recordcopy.recordID\n" +
                "JOIN\n" +
                "    artist ON record.artistID = artist.artistID\n" +
                "JOIN\n" +
                "    recordshop ON recordcopy.recordshopID = recordshop.recordshopID\n" +
                "WHERE\n" +
                "    artist.lastname = ?\n" +
                "    AND recordshop.city = ?\n" +
                "GROUP BY\n" +
                "    record.title,\n" +
                "    record.label,\n" +
                "    record.genre,\n" +
                "    record.rrp;"; //TO BE COMPLETED- Update this line as needed.
		
		
		try {
			//Connet to the database
			//TO BE COMPLETED
            String dbURL = URL;
            String user = USERNAME;
            String password = PASSWORD;

            Connection con = DriverManager.getConnection(dbURL, user, password);

			//Make the query
			//TO BE COMPLETED
            String artistLastName = requestStr[0];
            String recordShopCity = requestStr[1];

            PreparedStatement pstmt = con.prepareStatement(sql);

            pstmt.setString(1, artistLastName);
            pstmt.setString(2, recordShopCity);

            ResultSet rs = pstmt.executeQuery();

			//Process query
			//TO BE COMPLETED -  Watch out! You may need to reset the iterator of the row set.
            RowSetFactory factory = RowSetProvider.newFactory();
            CachedRowSet crs = factory.createCachedRowSet();
            crs.populate(rs);

            while (crs.next()) {
                System.out.println(crs.getString("title") + " | " +
                        crs.getString("label") + " | " +
                        crs.getString("genre") + " | " +
                        crs.getDouble("rrp") + " | " +
                        crs.getInt("copies_available"));
            }

            crs.beforeFirst();

            this.outcome = crs;

			//Clean up
			//TO BE COMPLETED
            rs.close();
            pstmt.close();
            con.close();


		} catch (Exception e)
		{ System.out.println(e); }

        return flagRequestAttended;
    }



    //Wrap and return service outcome
    public void returnServiceOutcome(){
        try {
			//Return outcome
			//TO BE COMPLETED
			OutputStream outputStream = this.serviceSocket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(this.outcome);
            objectOutputStream.flush();


            System.out.println("Service thread " + this.getId() + ": Service outcome returned; " + this.outcome);
            
			//Terminating connection of the service socket
			//TO BE COMPLETED
			this.serviceSocket.close();
			
        }catch (IOException e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
    }


    //The service thread run() method
    public void run()
    {
		try {
			System.out.println("\n============================================\n");
            //Retrieve the service request from the socket
            this.retrieveRequest();
            System.out.println("Service thread " + this.getId() + ": Request retrieved: "
						+ "artist->" + this.requestStr[0] + "; recordshop->" + this.requestStr[1]);

            //Attend the request
            boolean tmp = this.attendRequest();

            //Send back the outcome of the request
            if (!tmp)
                System.out.println("Service thread " + this.getId() + ": Unable to provide service.");
            this.returnServiceOutcome();

        }catch (Exception e){
            System.out.println("Service thread " + this.getId() + ": " + e);
        }
        //Terminate service thread (by exiting run() method)
        System.out.println("Service thread " + this.getId() + ": Finished service.");
    }

}
