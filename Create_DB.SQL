-- 
-- -------------------------------------------------------------------------------------------------
-- File:  Create_DB.SQL    Author: Jeffery A. Solheim     Date: January, 2015
-- This file contains a PostgreSQL script that, when run, creates the tables of the Reg_Ex schema.
-- -------------------------------------------------------------------------------------------------
-- 
DROP SCHEMA IF EXISTS Reg_Ex CASCADE ;
CREATE SCHEMA Reg_Ex ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Reg_Ex.Competition
  ( ID          SERIAL     PRIMARY KEY,
    StartTime   TIMESTAMP  NOT NULL,
    Name        VARCHAR(45) ) ;
ALTER SEQUENCE Reg_Ex.Competition_Id_Seq RESTART WITH 11 ;
Insert Into Reg_Ex.Competition ( StartTime, Name )
Values      ( '2015-01-29 15:30','2015 Spring MACS Seminar' ) ;
SELECT * FROM Reg_Ex.Competition ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Reg_Ex.Competitor
  ( ID     SERIAL       PRIMARY KEY,
    Name   VARCHAR(15)  UNIQUE
                        CHECK ( (Name IS NOT NULL) AND (LENGTH(Name) >= 1) ) ) ;
ALTER SEQUENCE Reg_Ex.Competitor_Id_Seq RESTART WITH 1001 ;
-- Insert Into Reg_Ex.Competitor ( Name ) Values ( 'Jeffery S' ) ;
SELECT * FROM Reg_Ex.Competitor ;
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Reg_Ex.Exercise
  ( ID           SERIAL        PRIMARY KEY,
    Pattern      VARCHAR(80)   CHECK ( (Pattern IS NOT NULL) AND (LENGTH(Pattern) >= 1) ),
    Description  VARCHAR(240)  CHECK ( (Description IS NOT NULL) AND (LENGTH(Description) >= 1) ) ) ;
ALTER SEQUENCE Reg_Ex.Exercise_Id_Seq RESTART WITH 1 ;
Insert Into    Reg_Ex.Exercise ( Pattern, Description )
Values         ( '(a)(a|b)*(b)',
                 'Strings over { <span style="color:blue;">a</span>, <span style="color:blue;">b</span> } that begin with an &nbsp; <span style="color:blue;">a</span> &nbsp; &amp; end with a &nbsp; <span style="color:blue;">b</span>' ) ;
Insert Into    Reg_Ex.Exercise ( Pattern, Description )
Values         ( '()|((a|b)(a|b))|((a|b)(a|b)(a|b))',
                 'Strings over { <span style="color:blue;">a</span>, <span style="color:blue;">b</span> } that are of length 0, 2, or 3' ) ;
Insert Into    Reg_Ex.Exercise ( Pattern, Description )
Values         ( '(a)|(a(a|b)*a)',
                 'Strings over { <span style="color:blue;">a</span>, <span style="color:blue;">b</span> } that both begin &amp; end with &nbsp; <span style="color:blue;">a</span>' ) ;
Insert Into    Reg_Ex.Exercise ( Pattern, Description )
Values         ( 'b*ab*ab*(()|a)b*',
                 'Strings over { <span style="color:blue;">a</span>, <span style="color:blue;">b</span> } that have exactly 2 or 3 &nbsp; <span style="color:blue;">a</span>''s' ) ;
-- Insert Into    Reg_Ex.Exercise ( Pattern, Description )
-- Values         ( 'b*(abb*)*(()|a)',
--                  'Strings over { a, b } that do not contain consecutive &nbsp; a''s' ) ;
-- Insert Into    Reg_Ex.Exercise ( Pattern, Description )
-- Values         ( 'a(a|b)a(((a|b)a)*)(()|(a|b))',
--                  'Strings over { a, b } of length at least 3 such that every other letter, beginning with the first, is an &nbsp; a' ) ;
-- Insert Into    Reg_Ex.Exercise ( Pattern, Description )
-- Values         ( '()|(a)|(b)|((((a)|(b))*)((aa)|(ba)|(bb)))',
--                  'Strings over { a, b } that do not end in &nbsp; ab' ) ;
-- Insert Into    Reg_Ex.Exercise ( Pattern, Description )
-- Values         ( '(a*)|(b(bb)*)|((()|(b(bb)*))(((aa*)(b(bb)*))*)(a*))',
--                  'Strings over { a, b } such that each &nbsp; b &nbsp; that occurs appears within a group of exactly an odd number of consecutive &nbsp; b''s' ) ;
SELECT * FROM Reg_Ex.Exercise ;
-- -------------------------------------------------------------------------------------------------
-- Competitors attempt/complete Exercises
-- -------------------------------------------------------------------------------------------------
CREATE TABLE Reg_Ex.Attempt
  ( ID             SERIAL       PRIMARY KEY,
    Competitor_ID  INT          NOT NULL  REFERENCES Reg_Ex.Competitor(ID),
    Exercise_ID    INT          NOT NULL  REFERENCES Reg_Ex.Exercise(ID),
    TimeSubmitted  TIMESTAMP    NOT NULL,
    RegExString    VARCHAR(80)  NOT NULL,
    IsSolution     BOOLEAN      NOT NULL,
    Error          VARCHAR(120)  ) ;
ALTER SEQUENCE Reg_Ex.Attempt_Id_Seq RESTART WITH 1001 ;
-- -------------------------------------------------------------------------------------------------
SELECT * FROM Reg_Ex.Attempt ;
-- -------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION  Reg_Ex.Exer_Solved_By  ( Competitor_ID_ARG INTEGER )
  RETURNS TABLE ( Exercise_ID INTEGER ) AS $$
    SELECT    Exercise_ID
    FROM      Reg_Ex.Attempt
    WHERE     ( Competitor_ID = Competitor_ID_ARG )
    AND       ( IsSolution )
    ORDER BY  Exercise_ID ASC
$$  LANGUAGE SQL ;
-- -------------------------------------------------------------------------------------------------
SELECT * FROM Reg_Ex.Exer_Solved_By ( 1002 ) ;
-- -------------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION Reg_Ex.Unsolved_Attempts_By ( Competitor_ID_ARG INTEGER ) 
  RETURNS TABLE  (  C_ID INTEGER,        C_Name TEXT,
                    E_ID INTEGER,        E_Pattern TEXT,  E_Description TEXT,
                    RegExString TEXT,    Error TEXT  ) AS $$
      SELECT      C.ID AS C_ID,                    C.Name AS C_Name,
                  E.ID AS E_ID,                    E.Pattern AS E_Pattern,
                  E.Description AS E_Description,  A.RegExString,  A.Error
      FROM        Reg_Ex.Competitor C
      CROSS JOIN  Reg_Ex.Exercise E
      LEFT JOIN   Reg_Ex.Attempt A
      ON          ( (C.ID = A.Competitor_ID) AND (E.ID = A.Exercise_ID) )
      WHERE       ( C.ID = Competitor_ID_ARG )
      AND         ( E.ID NOT IN
                         (SELECT Exercise_ID
                          FROM   Reg_Ex.Attempt
                          WHERE ( Competitor_ID = Competitor_ID_ARG ) AND ( IsSolution )) )
      ORDER BY    E.ID ASC, TimeSubmitted DESC
$$  LANGUAGE SQL ;
-- -------------------------------------------------------------------------------------------------
SELECT * FROM Reg_Ex.Unsolved_Attempts_By ( 1001 ) ;
-- -------------------------------------------------------------------------------------------------
SELECT * FROM Reg_Ex.Unsolved_Attempts_By ( 1001 ) LIMIT 1 ;
-- -------------------------------------------------------------------------------------------------
SELECT      C.Name AS Competitor_Name,
            E.ID AS Exercise_ID,
            E.Description AS Exercise_Description,
            A.TimeSubmitted  AS Time_Submitted
FROM        Reg_Ex.Competitor C
CROSS JOIN  Reg_Ex.Exercise E
LEFT JOIN   Reg_Ex.Attempt A
ON          ( (C.ID = A.Competitor_ID) AND (E.ID = A.Exercise_ID) )
WHERE       ( A.IsSolution )
ORDER BY    E.ID DESC, TimeSubmitted DESC
;
-- -------------------------------------------------------------------------------------------------
