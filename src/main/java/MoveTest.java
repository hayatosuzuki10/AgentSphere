import primula.agent.AbstractAgent;
import primula.api.AgentAPI;


public class MoveTest extends AbstractAgent {
  int saved_a = new Integer(0);
  boolean []BackupFlag = new boolean[ 4 ];
  int BACKUPNUM = new Integer(0);
  @ Override
  public void requestStop ( ) {
  }
  @ Override
  public synchronized void runAgent ( ) {
    ///螟画鋤蜑阪・繧ｳ繝ｼ繝・
    /*int a=0;
    print("1st");
    backup();
    print("2nd");
    backup();
    print("3rd");
    backup();
    while(a<3){
      ++a;
      print("loop:"+a);
      backup();
    }
    print("end");
 }
 void print(String s){
   try {
     wait(5000);
   } catch (InterruptedException e) {
     e.printStackTrace();
   }
   System.out.println(s);
 }*/
    int a = 0 ;
    if( BackupFlag [ 3 ] == false )
    {
      if( BackupFlag [ 2 ] == false ) {
        if( BackupFlag [ 1 ] == false ) {
          if( BackupFlag [ 0 ] == false ) {
            print ( "1st" ) ;
            saved_a = a ;
            BackupFlag [ 0 ] = true;
            if(AgentAPI.backup ( this , ++BACKUPNUM ,true) )return;
          }
          else {
            a = saved_a ;
            BackupFlag [ 0 ] = false ;
          }
          print ( "2nd" ) ;
          saved_a = a ;
          BackupFlag [ 1 ] = true;
          if(AgentAPI.backup ( this , ++BACKUPNUM ,true) )return;
        }
        else {
          a = saved_a ;
          BackupFlag [ 1 ] = false ;
        }
        print ( "3rd" ) ;
        saved_a = a ;
        BackupFlag [ 2 ] = true;
        if(AgentAPI.backup ( this , ++BACKUPNUM ,true) )return;
      }
      else {
        a = saved_a ;
        BackupFlag [ 2 ] = false ;
      }
    }
    while( BackupFlag [ 3 ] == true || a < 3 ) {
      if( BackupFlag [ 3 ] == false ) {
        ++ a ;
        print ( "loop:" + a ) ;
        saved_a = a ;
        BackupFlag [ 3 ] = true;
        if(AgentAPI.backup ( this , ++BACKUPNUM ,true) )return;
        BackupFlag [ 3 ] = false;
      }
      else {
        a = saved_a ;
        BackupFlag [ 3 ] = false ;
      }
    }
    print ( "end" ) ;
    if(AgentAPI.backup ( this , ++BACKUPNUM ,false) )return;
  }

  void print ( String s ) {
    try {
      wait ( 5000 ) ;
    }
    catch( InterruptedException e ) {
      e .printStackTrace( ) ;
    }
    System .out.println( s ) ;
  }
}
