/*
 *  File: RegExExerciseServlet.java
 *  Author: Jeffery A. Solheim      Date: January, 2015
 *  Definitions of doGet & doPost methods for .../RegExExercise URL.
 */

package servlet ;

// ------------------------------------------
import  javax.servlet.annotation.WebServlet ;
import  javax.servlet.http.* ;
import  java.io.PrintWriter ;
import  java.sql.* ;
import  java.net.* ;
import  java.util.regex.Pattern ;
import  java.util.regex.PatternSyntaxException ;
// ==========================================

@WebServlet  (  name = "RegExExerciseServlet",
                urlPatterns = {"/RegExExercise"}  )

public class RegExExerciseServlet extends HttpServlet
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
    // System.out.println("\n\nEntering doPost method of RegExExerciseServlet ...\n\n");
    // String sqlInsertString = null ;
    // int    insertRowCount  = -1 ;

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
        +  "<hr><h2>Example:  http<span style='color:red;'>s</span>://reg-ex.herokuapp.com/RegExExercise</h2>"
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

    // ------------------------------- session variables -------------------------------
    final  HttpSession  SESSION         = request.getSession() ;
    final  String       COMPETITOR_NAME = (String) SESSION.getAttribute ( "COMPETITOR_NAME" ) ;
    final  int          COMPETITOR_ID   = ((Integer) SESSION.getAttribute ( "COMPETITOR_ID" )).intValue() ;
    // =================================================================================

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

      // ---- determine whether there is any exercise this competitor has not yet solved ----
      boolean  existsUnsolved  =  false ;
      int      exerId          =  0 ;
      String   exerPatt        =  null ;
      String   exerDesc        =  null ;
      sqlString  =  "SELECT * FROM Reg_Ex.Unsolved_Attempts_By ( " + COMPETITOR_ID + " ) LIMIT 1 " ;
      rs = stmnt.executeQuery ( sqlString ) ;
      if ( rs.next() )
        {
        existsUnsolved  =  true ;
        exerId          =  rs.getInt("E_ID") ;
        exerPatt        =  rs.getString("E_Pattern") ;
        exerDesc        =  rs.getString("E_Description") ;
        }
      if ( rs != null )   { rs.close();   rs = null;  }
      // ====================================================================================

      if ( existsUnsolved ) // there is at least one exercise this competitor has not yet solved
        {
        // ---------- determine whether there is a regular expression to test ----------
        String    regExToTest  =  null ;
        String [] stringArray  =  request.getParameterValues ( "regExToTest" ) ;
        if ( (stringArray != null) && (stringArray.length > 0) )
          regExToTest = stringArray[0].replaceAll("\\s+","") ;
        // System.out.println("\n\nregExToTest is ... " + regExToTest + "\n\n");
        if ( regExToTest != null ) // there is a regular expression to test
          {
          // out.println ( "<h1>There is a Regular Expression to test, and it is \"" + regExToTest + "\".</h1><hr>" ) ;
          Attempt   currentAttempt = new Attempt ( COMPETITOR_ID, exerId, exerPatt, regExToTest ) ;
          String errorString = null ;
          if ( currentAttempt.getPatternSyntaxMessage() != null )
            errorString = currentAttempt.getPatternSyntaxMessage() ;
          if ( (errorString == null) && (currentAttempt.getCounterExample() != null) )
            errorString = currentAttempt.getCounterExample() ;
          if ( errorString == null )
            errorString = "NULL" ;
          else
            {
            errorString = errorString.replace("'","''");
            errorString = ("'" + errorString + "'") ;
            }
          sqlString  =
            "INSERT INTO Reg_Ex.Attempt " +
            "( Competitor_ID, Exercise_ID, TimeSubmitted, RegExString, IsSolution, Error ) " +
            "VALUES      ( " + COMPETITOR_ID + ", " + exerId + ", '" +
            currentAttempt.getTimeSubmitted() + "', '" + regExToTest +
            "', " + currentAttempt.getIsSolution() + ", " +
            errorString + " ) " ;
          // System.out.println("\n\nsqlString is ... \n\n");
          // sqlInsertString = sqlString ;
          // insertRowCount = stmnt.executeUpdate ( sqlString ) ;
          stmnt.executeUpdate ( sqlString ) ;

if ( currentAttempt.getIsSolution() )
  out.println ( "<h1>Your regular expression <span style='color:blue;'>" + regExToTest + "</span> passes all tests of Exercise " + exerId + ".</h1><hr>" ) ;

          // ---- update data regarding the first unsolved exercise ----
          existsUnsolved  =  false ;
          sqlString  =  "SELECT * FROM Reg_Ex.Unsolved_Attempts_By ( " + COMPETITOR_ID + " ) LIMIT 1 " ;
          rs = stmnt.executeQuery ( sqlString ) ;
          if ( rs.next() )
            {
            existsUnsolved  =  true ;
            exerId          =  rs.getInt("E_ID") ;
            exerPatt        =  rs.getString("E_Pattern") ;
            exerDesc        =  rs.getString("E_Description") ;
            }
          if ( rs != null )   { rs.close();   rs = null;  }
          // ===========================================================
          } // end if there was a regular expression to test

        // ---- if there is still an unsolved exercise, allow user to enter a guess ----
        if ( ! existsUnsolved )
          out.println ( "<h1>" + COMPETITOR_NAME + ", there are no additional unsolved exercises.</h1><hr>" ) ;
        else // there does exist at least one unsolved exercise
          {
          out.println ( "<h1>Exercise " + exerId + "</h1><hr>" ) ;
          out.println ( "<h2>" + exerDesc + "</h2>" ) ;

          // --------------------- generate several sample strings ---------------------
          Pattern regExPattern = null ;
          try
              { regExPattern = Pattern.compile ( exerPatt ) ; }
          catch ( PatternSyntaxException pse )
              { System.exit(1) ; }
          // ------------------------------
          String   examples = "" ;
          boolean  isFirst = true ;
          int      testNumber = 0 ;
          int      testCount = 0 ;
          // NOTE: FOLLOWING WHILE LOOP WILL BE AN INFINITE LOOP
          // IF THE GIVEN LANGUAGE IS OF CARDINALITY LESS THAN 12!
          while ( testCount < 12 )
            {
            // System.out.println("testNumber is ... <" + testNumber + ">");
            int strLen = -1 ;
            int x = testNumber + 1 ;
            while ( x > 0 )
              {
              x /= 2 ;
              strLen ++ ;
              } // end while loop
            int twoToTheStrLen = 1 ;
            for ( int i = 0 ; i < strLen ; i ++ )
              twoToTheStrLen *= 2 ;
            String testString = "" ;
            if ( strLen > 0 )
              {
              testString = String.format
                (("%"+strLen+"s"), Integer.toBinaryString(testNumber + 1 - twoToTheStrLen)) ;
              testString = testString.replace ( ' ', '0' ) ;
              testString = testString.replace ( '0', 'a' ) ;
              testString = testString.replace ( '1', 'b' ) ;
              }
            // System.out.println("testString is ... <" + testString + ">");
            // does current testString match target Reg Ex ?
            boolean testMatchesTarget = regExPattern.matcher(testString).matches() ;
            if ( testMatchesTarget )
              {
              testCount ++ ;
              if ( testString.length() <= 0 )
                testString = "&lambda;" ;
              examples += (( isFirst ? "" : ", &nbsp; " ) + testString) ;
              isFirst = false ;
              }
            testNumber ++ ;
            } // end while
          // ==============================
          out.println ( "<h2>Examples include &nbsp; &#9758 &nbsp; " + examples + "</h2><hr>" ) ;
          // ===========================================================================

          // ------------ display form in which next attempt can be entered ------------
          out.print   ( ""
            +  "    <form action='RegExExercise' method='POST'>\n"
            +  "      <fieldset style=\n"
            +  "       'border-style:solid;border-color:red;font-weight:bold;font-style:italic;font-size:12pt;width:95%;' >\n"
            +  "        <legend style='font-style:italic;text-align:center;'>\n"
            +  "          &nbsp;&nbsp;Enter Your Guess at the Regular Expression Below&nbsp;&nbsp;\n"
            +  "        </legend>\n"
            +  "        <input type='text' name='regExToTest' style='width:99%;font-size:40px;' autofocus><br><hr>\n"
            +  "        <input type='submit' value='Click Here to Submit Your Regular Expression for Testing' style='width:100%;'>\n"
            +  "      </fieldset>\n"
            +  "    </form><hr>\n" ) ;
          // ===========================================================================

          // ---- display this competitor's previous attempts to solve this exercise ----
          boolean  existPreviousAttempts  =  false ;
          sqlString  =     "SELECT * FROM Reg_Ex.Unsolved_Attempts_By ( " + COMPETITOR_ID + " ) "
                        +  "WHERE (E_ID = " + exerId + ") "
                        +  "AND   (RegExString IS NOT NULL) " ;
          rs = stmnt.executeQuery ( sqlString ) ;
          String previousAttemptsTable = "<table style='font-size:x-large;'>\n<caption>Previous Attempts (Most Recent at Top)</caption>\n" ;
          while ( rs.next() )
            {
            // System.out.println("\n\npreviousAttemptsTable is ... <" + previousAttemptsTable + ">\n\n");
            existPreviousAttempts  =  true ;
            previousAttemptsTable += "<tr>\n" ;
            previousAttemptsTable += "<td>" + rs.getString("RegExString") + "</td>\n" ;
            previousAttemptsTable += "<td>" + rs.getString("Error") + "</td>\n" ;
            previousAttemptsTable += "</tr>\n" ;
            } // end while
          previousAttemptsTable += "</table>\n" ;
          if ( rs != null ) { rs.close(); rs = null; }
          if ( existPreviousAttempts )
            out.println ( previousAttemptsTable + "<hr>" ) ;
          // ============================================================================
          } // end if existsUnsolved
        // =============================================================================
        }
      else
        out.println ( "<h1>" + COMPETITOR_NAME + ", there are no additional unsolved exercises.</h1><hr>" ) ;
      } // end try block for database access
    catch ( URISyntaxException use )
      { use.printStackTrace() ; }
    catch ( ClassNotFoundException cnfe )
      { cnfe.printStackTrace() ; }
    catch ( SQLException sqle )
      { sqle.printStackTrace() ; }
    finally
      {
      // System.out.println("\n\nin finally block ... \n\n");
      // ------------------  complete HTML webpage  --------------------
      // out.println ( "<h1>sqlInsertString == " + sqlInsertString + "</h1><hr>" ) ;
      // out.println ( "<h1>insertRowCount == " + insertRowCount + "</h1><hr>" ) ;
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

  } // end RegExExerciseServlet class
