/*
 *  File: LeaderBoardServlet.java
 *  Author: Jeffery A. Solheim      Date: January, 2015
 *  Definitions of doGet & doPost methods for .../LeaderBoard URL.
 */

package servlet ;

// ------------------------------------------
import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;
import  java.sql.* ;
import  java.net.* ;
// ==========================================

@WebServlet  (  name = "LeaderBoardServlet",
                urlPatterns = {"/LeaderBoard"}  )

public class LeaderBoardServlet extends HttpServlet
  {

  @Override
  public synchronized void doGet ( final HttpServletRequest  request,
                                   final HttpServletResponse response )
      throws javax.servlet.ServletException, java.io.IOException
    {
    this.doPost ( request, response ) ;
    } // end doGet method

  @Override
  protected synchronized void doPost ( HttpServletRequest  request,
                                       HttpServletResponse response )
      throws javax.servlet.ServletException, java.io.IOException
    {
    // --------- prepare PrintWriter for HTML output ---------
    response.setContentType ( "text/html" ) ;
    final PrintWriter out = response.getWriter() ;
    // =======================================================

    // -----------------  print first portion of HTML webpage  ----------------
    out.println ( "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n"
      +  "<style>\n"
      +  "  table, th, td  { border:1px solid black; \n"
      +  "                   padding-left:10px; padding-right:10px; \n"
      +  "                   padding-top:4px; padding-bottom:4px; } \n"
      +  "</style>\n"
      +  "<title>Regular Expression Competition</title>\n</head>\n<body>\n<hr>" ) ;
    // =======================================================================

    // ----------------------  check for use of HTTPS  ----------------------
    final String originatingProtocol = request.getHeader("X-Forwarded-Proto") ;
    if ( ! (originatingProtocol.equals("https")) )
      {
      out.println
        (  "<hr><h2>URI scheme must be http<span style='color:red;'>s</span>.</h2>"
        +  "<hr><h2>Example:  http<span style='color:red;'>s</span>://reg-ex.herokuapp.com/LeaderBoard</h2>"
        +  "<hr></body></html>"  ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if protocol not https
    // =======================================================================

    // ----------------------  check for use of CHROME  ----------------------
/*
    final String userAgent = request.getHeader("User-Agent") ;
    if ( (userAgent == null) || (! (userAgent.contains("Chrome"))) )
      {
      out.println
        (  "<hr><h2>This application was developed &amp; tested using the browser Google Chrome, Version 39.0.2171.95 m.</h2>"
        +  "<hr><h2>You may download the Chrome web browser from http://www.Google.com/Chrome.</h2>"
        +  "<hr></body></html>"  ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if userAgent not Chrome
*/
    // =======================================================================

    // -------------------------- database-related variables --------------------------
    Connection         dbConn         =  null ;
    Statement          stmnt          =  null ;
    String             sqlString      =  null ;
    ResultSet          rs             =  null ;
    // =================================================================================

    // ----------------------------- begin database access -----------------------------
    try
      {
      // ---------------------------- connect to database ----------------------------
      URI dbUri = new URI(System.getenv("DATABASE_URL")) ;
      String username = dbUri.getUserInfo().split(":")[0] ;
      String password = dbUri.getUserInfo().split(":")[1] ;
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() ;
      Class.forName ( "org.postgresql.Driver" ) ;
      dbConn = DriverManager.getConnection ( dbUrl, username, password ) ;
      stmnt = dbConn.createStatement() ;
      // =============================================================================

      // ---------------------------- generate table of leaders ----------------------------
      sqlString  =  ""
                 +  "SELECT      C.Name AS Competitor_Name, "
                 +  "            E.ID AS Exercise_ID, "
                 +  "            E.Description AS Exercise_Description, "
                 +  "            A.TimeSubmitted  AS Time_Submitted "
                 +  "FROM        Reg_Ex.Competitor C "
                 +  "CROSS JOIN  Reg_Ex.Exercise E "
                 +  "LEFT JOIN   Reg_Ex.Attempt A "
                 +  "ON          ( (C.ID = A.Competitor_ID) AND (E.ID = A.Exercise_ID) ) "
                 +  "WHERE       ( A.IsSolution ) "
                 +  "ORDER BY    E.ID DESC, TimeSubmitted DESC "  ;
      rs = stmnt.executeQuery ( sqlString ) ;
      String leaderBoardTable = "<table style='font-size:xx-large;'>\n<caption>Solved Exercises</caption>\n" ;
      leaderBoardTable += "<tr><th>Competitor</th><th>Exercise</th><th>Time Solved</th></tr>\n" ;
      while ( rs.next() )
        {
        leaderBoardTable += "<tr>\n" ;
        leaderBoardTable += "<td>" + rs.getString("Competitor_Name") + "</td>\n" ;
        leaderBoardTable += "<td>" + rs.getString("Exercise_ID") + "</td>\n" ;
        leaderBoardTable += "<td>" + rs.getString("Time_Submitted") + "</td>\n" ;
        leaderBoardTable += "</tr>\n" ;
        } // end while loop
      if ( rs != null )   { rs.close();   rs = null;  }
      leaderBoardTable += "</table>\n" ;
      out.println ( leaderBoardTable + "<hr>" ) ;
      // ===================================================================================
      } // end try block for database access
    catch ( URISyntaxException use )
      { use.printStackTrace() ; }
    catch ( ClassNotFoundException cnfe )
      { cnfe.printStackTrace() ; }
    catch ( SQLException sqle )
      { sqle.printStackTrace() ; }
    finally
      {
      // ------------------  complete HTML webpage  --------------------
      out.println ( "</body>\n</html>" ) ;
      out.flush() ;
      out.close() ;
      // ===============================================================
      try
        {
        if ( stmnt      !=  null )  { stmnt.close();       stmnt = null;      }
        if ( dbConn     !=  null )  { dbConn.close();      dbConn = null;     }
        }
      catch ( SQLException sqle )
        { sqle.printStackTrace() ; }
      } // end finally block after database access
    // =============================  end database access  =============================
    } // end doPost method

  } // end LeaderBoardServlet class
