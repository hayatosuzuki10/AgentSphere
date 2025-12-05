import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import primula.agent.AbstractAgent;
import primula.api.AgentAPI;
import primula.api.MessageAPI;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.util.IPAddress;
import primula.util.KeyValuePair;


@SuppressWarnings("serial")
public class MSMaster extends AbstractAgent implements IMessageListener{

	boolean gamePlay = true;
	boolean gameClear = true;
	int mWidth = 900; // width of the minefield   #microsoft30
	int mHeight = 480; // height of the minefield #microsoft16
	int mMines = 27000; // number of mines        #microsoft99
	char[] mMinefield; // 2-dimensional array of MSBoards for our board
	int[] mineNum;
	boolean[] openFlag;
	boolean[] mineFlag;
	List<Hint> HintList = new ArrayList<Hint>();
	int clearCount = 0;
	int playCount = 0;
	int burstCount = 0;

	long totalTime = 0;
	long StartTime = 0;
	long tempTime1 = 0;
	long tempTime2 = 0;
	long CalculateTime = 0;
	long CommunicateTime = 0;
	long migrationTime = 0;
	long resultTime = 0;

	long pack[] = new long[3];

	 MSSlave slave1=new MSSlave();
	 //tspdfs18 slave2=new tspdfs18();
	 //tspdfs18 slave3=new tspdfs18();
	 //tspdfs18 slave4=new tspdfs18();
	 //tspdfs18 slave5=new tspdfs18();
	 //tspdfs18 slave6=new tspdfs18();
	 //tspdfs18 slave7=new tspdfs18();

	 	KeyValuePair<InetAddress, Integer> ToSlave1 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave2 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave3 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave4 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave5 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave6 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave7 = null;
		//KeyValuePair<InetAddress, Integer> ToSlave8 = null;

	 	//ここから
	public void run(){
		try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Agent Name:" + this.getAgentName());



		try {
			ToSlave1 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave1),55878);
			//ToSlave2 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave2),55878);
			//ToSlave3 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave3),55878);
			//ToSlave4 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave4),55878);
			//ToSlave5 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave5),55878);
			//ToSlave6 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave6),55878);
			//ToSlave7 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave7),55878);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// int number_of_nodes = 0;
		// int adjacency_matrix[][] = null;
         //Scanner scanner = null;


		  //Generating field
		  System.out.println("M: Generating minefield...");
		  setMinefield();

		  System.out.println("M: Clearing minefield...");
		  clearMinefield();

		  System.out.println("M: Placing mines...");
		  placeMines();
		  //drawMinefield();

		  System.out.println("M: Calculating hints...");
		  calculateHints();
		  //drawMinefield();

		  //System.out.println("Open minefield ...");
		  //drawOpenfield();

		  //Slaveに送る
		  System.out.println("M: Sending -> S");
		  StartTime = System.currentTimeMillis();
		  slave1.setattr(mMinefield, mineNum, getStrictName(), 0);

		  tempTime1 = System.currentTimeMillis();
		  AgentAPI.migration(ToSlave1, slave1);
		  tempTime2 = System.currentTimeMillis();

		  migrationTime = (tempTime2-tempTime1);

		  System.out.println("M: Sent -> S");
		  System.out.println();

		  while(gamePlay){
			  try{
				  Thread.sleep(10000);
		      }catch (InterruptedException e){
		      }
		  }
		  System.out.println("M: Finished.");

	}
	//ここまで

	public class Hint {
		public List<Integer> hintSquare;
		public int mNum;

		public Hint(){
			hintSquare = new ArrayList<Integer>();
		}
	}

	public void setMinefield(){
		mMinefield = new char[mHeight*mWidth];
		mineNum = new int[mHeight*mWidth];
		openFlag = new boolean[mHeight*mWidth];
		mineFlag = new boolean[mHeight*mWidth];
	}

	public void clearMinefield() {
	    // Set every grid space to a space character.
	    for(int i = 0; i < mHeight*mWidth; i ++) {
	        mMinefield[i] = ' ';
	        mineNum[i] = 0;
	        openFlag[i] = false;
	        mineFlag[i] = false;
	        HintList.clear();
	    }
	}

	public void placeMines() {
		int minesPlaced = 0;
	    Random random = new Random(2434); // this generates random numbers for us
	    while(minesPlaced < mMines) {
	    	int x = random.nextInt(mHeight*mWidth); // a number between 0 and mWidth*mHeight - 1
	    	// make sure we don't place a mine on top of another
	    	if(x != 0 && x != 1 &&  x != mWidth && x != mWidth+1 && mMinefield[x] != '*') {
	    		mMinefield[x] = '*';
	    		mineNum[x] = -1;
	    		minesPlaced ++;
	    	}
	    }
	}

	public void calculateHints() {
	    for(int i = 0; i < mHeight*mWidth; i ++) {
	    	if(mMinefield[i] != '*') {
	        	int n = minesNear(i);
	        	if(n != 0){
	        		mMinefield[i] = (char)(n+48);
	        	}else{
	        		mMinefield[i] = ' ';
	        	}
	        	mineNum[i] = n;
	    	}
	    }
	}

	public int minesNear(int i) {
	    int mines = 0;
	    // check mines in all directions
	    if((i+1)%mWidth != 0){
		    mines += mineAt(i - mWidth + 1);  // NE
		    mines += mineAt(i + 1);      // E
		    mines += mineAt(i + mWidth + 1);  // SE
	    }
	    if(i%mWidth != 0){
	    	mines += mineAt(i - mWidth - 1);  // NW
	    	mines += mineAt(i - 1);      // W
	    	mines += mineAt(i + mWidth - 1);  // SW
	    }
	    mines += mineAt(i - mWidth);      // N
	    mines += mineAt(i + mWidth);      // S
	    return mines;
	}

	// returns 1 if there's a mine a y,x or 0 if there isn't
	public int mineAt(int i) {
		// we need to check also that we're not out of array bounds as that would
	    // be an error
	    if(i >= 0 && i < mHeight*mWidth && mMinefield[i] == '*') {
	    	return 1;
	    } else {
	    	return 0;
	    }
	}

	public void drawMinefield() {
	    for(int i = 0; i < mHeight*mWidth; i ++) {
	    	System.out.print(mMinefield[i]);
	    	if((i+1)%mWidth == 0){
	    		System.out.print("\n");
	    	}
	    }
	}

	public void drawOpenfield(){
		for(int i = 0; i < mHeight*mWidth; i ++) {
			if(openFlag[i] == true){
				System.out.print(mMinefield[i]);
			}else{
				System.out.print("-");
			}
			if((i+1)%mWidth == 0){
				System.out.print("\n");
			}
		}
	}

	  public void openFirst(){
		  openFlag[0] = true;
/*
		  if(mMinefield[0] == '*'){
			  gamePlay = false;
			  gameClear = false;
			  return;
		  }
*/
		  HintList.add(hintSet(0));
	  }

	  public void openSquare(){
		  for(int i = 0 ; i < HintList.size() ; i++){
			  if(HintList.get(i).mNum == 0){
				  int num = HintList.get(i).hintSquare.get(0);
				  openFlag[num] = true;
				  //hintCheck(num);

				  if(mMinefield[num] == '*'){
					  openFlag[num] = false;
					  burstCount++;
					  mineFlag[num] = true;
					  hintCheck(num);
					  //gamePlay = false;
					  gameClear = false;
					  Hint h = new Hint();
					  h.hintSquare.add(num);
					  h.mNum = 1;
					  HintList.add(h);
					  return;
				  }

				  hintCheck(num);
				  HintList.add(hintSet(num));
				  HintList.remove(i);
				  return;
			  }
		  }

		  for(int j = 0 ; j < mHeight*mWidth ; j++){
			  if(openFlag[j] == false && mineFlag[j] == false){
				  openFlag[j] = true;
				  //hintCheck(j);

				  if(mMinefield[j] == '*'){
					  openFlag[j] = false;
					  burstCount++;
					  mineFlag[j] = true;
					  hintCheck(j);
					  //gamePlay = false;
					  gameClear = false;
					  Hint h = new Hint();
					  h.hintSquare.add(j);
					  h.mNum = 1;
					  HintList.add(h);
					  return;
				  }

				  hintCheck(j);
				  HintList.add(hintSet(j));
				  return;
			  }
		  }
	  }

	  public Hint hintSet(int i){
		  Hint h = new Hint();

		  if((i+1)%mWidth != 0){
			  if((i-mWidth) >= 0 && openFlag[i-mWidth+1] == false) h.hintSquare.add(i - mWidth + 1);
			  if(openFlag[i+1] == false) h.hintSquare.add(i + 1);
			  if((i+mWidth) < mHeight*mWidth && openFlag[i+mWidth+1] == false) h.hintSquare.add(i + mWidth + 1);
		  }
		  if(i%mWidth != 0){
			  if((i-mWidth) >= 0 && openFlag[i-mWidth-1] == false) h.hintSquare.add(i - mWidth - 1);
			  if(openFlag[i-1] == false) h.hintSquare.add(i - 1);
			  if((i+mWidth) < mHeight*mWidth && openFlag[i+mWidth-1] == false) h.hintSquare.add(i + mWidth - 1);
		  }
		  if((i-mWidth) >= 0 && openFlag[i-mWidth] == false) h.hintSquare.add(i - mWidth);
		  if((i+mWidth) < mHeight*mWidth && openFlag[i+mWidth] == false) h.hintSquare.add(i + mWidth);

		  h.mNum = mineNum[i];

		  return h;
	  }

	  public void hintDivide(){
		  for(int i = 0 ; i < HintList.size() ; i++){
			  if(HintList.get(i).hintSquare.size() == HintList.get(i).mNum && HintList.get(i).mNum != 1){
				  for(int j = 0 ; j < HintList.get(i).hintSquare.size() ; j++){
					  Hint h = new Hint();
					  h.hintSquare.add(HintList.get(i).hintSquare.get(j));
					  h.mNum = 1;
					  HintList.add(h);
				  }
				  HintList.remove(i);
				  i--;
			  }else if(HintList.get(i).hintSquare.size() != 1 && HintList.get(i).mNum == 0){
				  for(int j = 0 ; j < HintList.get(i).hintSquare.size() ; j++){
					  Hint h = new Hint();
					  h.hintSquare.add(HintList.get(i).hintSquare.get(j));
					  h.mNum = 0;
					  HintList.add(h);
				  }
				  HintList.remove(i);
				  i--;
			  }
		  }
	  }

	  public void MineCheck(){
		  for(int i = 0 ; i < HintList.size() ; i++){
			  if(HintList.get(i).mNum == 1 && HintList.get(i).hintSquare.size() == 1){
				  int n = HintList.get(i).hintSquare.get(0);
				  mineFlag[n] = true;
				  HintList.remove(i);
				  hintCheck(n);
			  }
		  }
	  }

	  public void hintCheck(int n){
		  for(int i=0 ; i < HintList.size() ; i++){
			  for(int j=0 ; j < HintList.get(i).hintSquare.size() ; j++){
				  if(HintList.get(i).hintSquare.get(j) == n){
					  HintList.get(i).hintSquare.remove(j);
					  if(mineFlag[n] == true) HintList.get(i).mNum--;
				  }
			  }
		  }
	  }

	  public void playCheck(){
		  int count = 0;
		  for(int i = 0 ; i < mHeight*mWidth ; i++){
			  if(!openFlag[i]) count++;
		  }

		  if(count == mMines) gamePlay = false;
	  }

	  public void flagReset(){
		  gamePlay = true;
		  gameClear = true;
	  }


	@Override
	public void requestStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getStrictName() {
		// TODO Auto-generated method stub
		return this.getAgentID();
	}

	@Override
	public String getSimpleName() {
		// TODO Auto-generated method stub
		return this.getAgentName();
	}

	@Override
	public void receivedMessage(AbstractEnvelope envelope) {
		// TODO Auto-generated method stub

		long EndTime = System.currentTimeMillis();
		System.out.println("M: Received result <- S");
		StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
		pack=(long[]) cont.getContent();

		playCount++;
		System.out.println("M: " + playCount + "times finish");

		long playTime = (EndTime-StartTime);
		totalTime += playTime;
		CalculateTime = (totalTime - CommunicateTime - migrationTime);


		if((int)pack[1] == 0) gameClear = true;
		else gameClear = false;

		burstCount += (int)pack[2];

		if(gameClear == true){
			//drawMinefield();
			System.out.println("M: Game Clear!");
			clearCount ++;
		}else{
			//drawMinefield();
			System.out.println("M: Game Over...");
		}

		System.out.println("M: play time -> " + (playTime/1000.0) + "[s]");
		System.out.println();

		double clearRate = (double)clearCount/(double)playCount * 100;
		double burstAve = (double)burstCount/(double)playCount;
		System.out.println("M: clear count       : " + clearCount);
		System.out.println("M: play count        : " + playCount);
		System.out.println("M: clear rate        : " + clearRate + "[%]");
		System.out.println("M: burst count       : " + burstAve);
		System.out.println("M: clear time        : " + (totalTime/1000.0) + "[s]");
		System.out.println("M: communicate time  : " + (CommunicateTime/1000.0) + "[s]");
		System.out.println("M: mig start time    : " + (migrationTime/1000.0) + "[s]");
		System.out.println("M: culculate time    : " + (CalculateTime/1000.0) + "[s]");

		gamePlay = false;

	}
}
