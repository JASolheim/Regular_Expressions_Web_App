/*
 *  File: RegisterCompetitorNameServlet.java
 *  Author: Jeffery A. Solheim      Date: January, 2015
 *  This file contains definitions of doGet & doPost methods that are invoked for
 *  the .../RegisterCompetitorName URL.
 */

package servlet ;

import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;
import  java.sql.* ;
import  java.net.* ;

@WebServlet  (  name = "RegisterCompetitorNameServlet",
                urlPatterns = {"/RegisterCompetitorName"}  )

public class RegisterCompetitorNameServlet extends HttpServlet
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
    // prepare to write HTML output via a PrintWriter ...
    response.setContentType ( "text/html" ) ;
    final PrintWriter out = response.getWriter() ;

    // print the first portion of the HTML webpage ...
    out.println ( "<!DOCTYPE html>\n<html>\n<head>\n<meta charset='UTF-8'>\n<style>\n"
      +  "table, th, td  { border:1px solid black; "
      +  "                 padding-left:10px; padding-right:10px; "
      +  "                 padding-top:4px; padding-bottom:4px; } "
      +  "</style>\n<title>Regular Expressions</title>\n</head>\n<body>\n" ) ;

    final String originatingProtocol = request.getHeader("X-Forwarded-Proto") ;
    if ( ! (originatingProtocol.equals("https")) )
      {
      out.println
        (  "<hr><h2>URI scheme must be http<span style='color:red;'>s</span>.</h2>"
        +  "<hr><h2>Example:  http<span style='color:red;'>s</span>://reg-ex.herokuapp.com/RegisterCompetitorName</h2>"
        +  "<hr></body></html>"  ) ;
      out.flush() ;
      out.close() ;
      return ; // exit doPost method
      } // end if protocol not https

    // -------------------------------------------------------------
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
    // -------------------------------------------------------------

    String competitorName = request.getParameterValues ( "competitorName" ) [0] ;
    final HttpSession SESSION = request.getSession() ;
    SESSION.setAttribute ( "COMPETITOR_NAME", competitorName ) ;
    boolean competitorNameWasPreviouslyExtant = false ;
    String  competitionName = null ;
    String competitorTable = "<table>\n<caption>The Competitors</caption>" ;
    int rowCount = 0 ;

// ==============================================================================================
    // ------ database-related variables ------
    Connection         dbConn         =  null ;
    String             sqlString      =  null ;
    Statement          stmnt          =  null ;
    ResultSet          rs             =  null ;
    ResultSetMetaData  rsmd           =  null ;
    //String status = "Status:&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" ;
    try
      {
      // ---- connect to database ----
      URI dbUri = new URI(System.getenv("DATABASE_URL")) ;
      //status += ("dbUri=" + dbUri + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") ;
      String username = dbUri.getUserInfo().split(":")[0] ;
      //status += ("username=" + username + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") ;
      String password = dbUri.getUserInfo().split(":")[1] ;
      //status += ("password=" + password + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") ;
      String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() ;
      //status += ("dbUrl=" + dbUrl + "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") ;
      Class.forName ( "org.postgresql.Driver" ) ;
      dbConn = DriverManager.getConnection ( dbUrl, username, password ) ;
      //status += ("DID connect&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;") ;
      stmnt = dbConn.createStatement() ;

      sqlString  =  "SELECT    Name  FROM  Reg_Ex.Competition "
                 +  "ORDER BY  StartTime  DESC " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      if ( rs.next() )
        competitionName = rs.getString("Name") ;
      if ( rs != null ) { rs.close(); rs = null; }

      sqlString  =  "SELECT  Name FROM Reg_Ex.Competitor "
                 +  "WHERE   lower(Name) = lower('" + competitorName + "') " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      if ( rs.next() )
        competitorNameWasPreviouslyExtant = true ;
      if ( rs != null ) { rs.close(); rs = null; }

      if ( ! competitorNameWasPreviouslyExtant )
        {
        sqlString  =  "INSERT INTO Reg_Ex.Competitor ( Name ) "
                   +  "VALUES      ( '" + competitorName + "' ) " ;
        rowCount = stmnt.executeUpdate ( sqlString ) ;
        }

      sqlString  =  "SELECT  ID FROM Reg_Ex.Competitor "
                 +  "WHERE   lower(Name) = lower('" + competitorName + "') " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      if ( rs.next() )
        SESSION.setAttribute ( "COMPETITOR_ID", new Integer(rs.getInt("ID")) ) ;
      if ( rs != null ) { rs.close(); rs = null; }

      // ---- obtain competitor names from database ----
      sqlString  =  "SELECT * FROM Reg_Ex.Competitor ORDER BY Name " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      rsmd = rs.getMetaData();
      int numCols = rsmd.getColumnCount();
      while ( rs.next() )
        {
        competitorTable += "<tr>\n" ;
        //competitorTable += "<td>" + rs.getString("ID") + "</td>\n" ;
        competitorTable += "<td>" + rs.getString("Name") + "</td>\n" ;
        competitorTable += "</tr>\n" ;
        } // end while
      if ( rs != null ) { rs.close(); rs = null; }
      } // end try block
    catch ( URISyntaxException use )
      { use.printStackTrace() ; }
    catch ( ClassNotFoundException cnfe )
      { cnfe.printStackTrace() ; }
    catch ( SQLException sqle )
      { sqle.printStackTrace() ; }
    finally
      {
      try
        {
        if ( stmnt      !=  null )  { stmnt.close();       stmnt = null;      }
        if ( dbConn     !=  null )  { dbConn.close();      dbConn = null;     }
        }
      catch ( SQLException sqle )
        { sqle.printStackTrace() ; }
      } // end finally block
// ==============================================================================================

    if ( competitorNameWasPreviouslyExtant )
      {
      SESSION.invalidate() ;
      out.println ( "<hr>\n<h2>The name &nbsp;&nbsp; <span style='color:red;'>" + competitorName + "</span> &nbsp;&nbsp; was already taken.  Try again.</h2>\n<hr>\n"
        +  "</body>\n</html>\n"
        ) ;
      out.flush() ;
      out.close() ;
      return ;
      }

    competitorTable += "</table>\n" ;

    // print the remainder of the HTML webpage ...
    out.println ( "<hr>\n<h2>Welcome to the " + competitionName + " Regular Expression Competition, " + competitorName + "!</h2>\n<hr>\n"
      + "<ul style='font-size:x-large'>\n"
      + "  <li> &nbsp; &nbsp; &nbsp; &nbsp; For each regular language that will be described, you are to find a regular expression that generates exactly that language.</li>\n"
      + "  <li> &nbsp; &nbsp; &nbsp; &nbsp; &lambda;, a string of zero letters, should be entered as a pair of empty parentheses, like this: &nbsp;<span style='color:blue;'>()</span> &nbsp;.</li>\n"
      + "</ul>\n<hr>\n"
      //+ "<p>" + competitorName + ((competitorNameWasPreviouslyExtant) ? (" IS already extant") : (" is NOT already extant")) + "</p><hr>"
      //+ "<p>" + status + "</p><hr>"
      //+ "<p>Inserted rowCount = " + rowCount + "</p><hr>"
      +  competitorTable  +  "<hr>"
      +  "    <form action='RegExExercise' method='POST'>\n"
      +  "      <fieldset style=\n"
      +  "       'border-style:solid;border-color:red;font-weight:bold;font-style:italic;font-size:12pt;width:95%;' >\n"
      +  "        <legend style='font-style:italic;text-align:center;'>\n"
      +  "          &nbsp;&nbsp;Wait here until you are told to begin!&nbsp;&nbsp;\n"
      +  "        </legend>\n"
      +  "        <input type='submit' value='Click here only after you have been instructed to begin.' style='width:100%;'>\n"
      +  "      </fieldset>\n"
      +  "    </form><hr>\n"
      +  "</body>\n"
      +  "</html>\n"
      ) ;
    out.flush() ;
    out.close() ;
    } // end doPost method

  } // end RegisterCompetitorNameServlet class
