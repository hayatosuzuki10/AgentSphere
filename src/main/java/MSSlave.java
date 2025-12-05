import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import primula.agent.AbstractAgent;
import primula.api.MessageAPI;
import primula.api.core.network.AgentAddress;
import primula.api.core.network.message.AbstractEnvelope;
import primula.api.core.network.message.IMessageListener;
import primula.api.core.network.message.StandardContentContainer;
import primula.api.core.network.message.StandardEnvelope;
import primula.util.IPAddress;
import primula.util.KeyValuePair;



	@SuppressWarnings("serial")
public class MSSlave extends AbstractAgent implements IMessageListener {

	boolean gamePlay = true;
	boolean gameClear = true;
	int mWidth = 900; // width of the minefield   #microsoft30
	int mHeight = 480; // height of the minefield #microsoft16
	int mMines = 27000; // number of mines        #microsoft99
	char[] mMinefield = new char[mHeight*mWidth]; // 2-dimensional array of MSBoards for our board
	int[] mineNum = new int[mHeight*mWidth];
	boolean[] openFlag = new boolean[mHeight*mWidth];
	boolean[] mineFlag = new boolean[mHeight*mWidth];
	List<Hint> HintList = new ArrayList<Hint>();
	int clearCount = 0;
	int playCount = 0;
	int burstCount = 0;

	long totalTime = 0;
	long tempTime1 = 0;
	long tempTime2 = 0;

	int startpoint = 0;
	int openCount = 0;
	int turnCount = 0;

	long[] pack = new long[3];

	String parentid;

    public void setattr (char[] minefield, int[] minenum, String parent, int i){

    	this.mMinefield=minefield;
    	this.mineNum=minenum;
    	this.parentid=parent;
    	this.startpoint=i;

    }

    public MSSlave() {
    	super();
		// TODO Auto-generated constructor stub
	}


 //ここから
	public void run(){

		KeyValuePair<InetAddress, Integer> address = null;
		try {
			address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Master),55878);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//this.migrate();
		try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        long StartTime = System.currentTimeMillis();
        //System.out.println("Enter the number of nodes in the graph");

        try{
            System.out.println("S: Waiting for master...");
           	Thread.sleep(3000); //3000ミリ秒Sleepする
        }catch(InterruptedException e){}

        //System.out.println();

        System.out.println("S: Solving minefield...");

		openFirst();
		//drawOpenfield();

		while(gamePlay){

			turnCount++;

			if(turnCount%10000 == 0) System.out.println("S: Solving minefield...");

			hintDivide();
			MineCheck();
			hintDivide();
			//hintCompre();
			openSquare();
			//drawOpenfield();
			playCheck();
			//System.out.println((openCount++) + "times open");
		}

		long EndTime = System.currentTimeMillis();

		pack[0] = (EndTime - StartTime);

		if(gameClear) pack[1] = 0;
		else pack[1] = 1;

		pack[2] = burstCount;

		System.out.println("S: Sending -> M");
        StandardEnvelope envelope = new StandardEnvelope(new AgentAddress(parentid), new StandardContentContainer(pack));

        tempTime1 = System.currentTimeMillis();
		MessageAPI.send(address, envelope);
		tempTime2 = System.currentTimeMillis();

		System.out.println("S: Sent -> M");
		System.out.println();

		System.out.println("S1: result rep time   : " + ((tempTime2-tempTime1)/1000.0) + "[s]");

		while(playCount != 500){
			  try{
				  Thread.sleep(10000);
		      }catch (InterruptedException e){
		      }
		}

	}
	//ここまで

	public class Hint {
		public List<Integer> hintSquare;
		public int mNum;

		public Hint(){
			hintSquare = new ArrayList<Integer>();
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
/*
		System.out.println("S: Received new board <- M");
		StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
		MSSlave NBoard=(MSSlave) cont.getContent();

		this.mMinefield = NBoard.mMinefield;
		this.mineNum = NBoard.mineNum;
		this.parentid = NBoard.parentid;
		for(int i = 0 ; i < this.mHeight*this.mWidth ; i++) {
			this.openFlag[i] = false;
			this.mineFlag[i] = false;
		}
		this.burstCount = 0;
		this.HintList.clear();

		flagReset();

		long StartTime = System.currentTimeMillis();
        //System.out.println("Enter the number of nodes in the graph");

		/*
        try{
            System.out.println("Waiting for master...");
           	Thread.sleep(3000); //3000ミリ秒Sleepする
        }catch(InterruptedException e){}
		*/

        //System.out.println();
/*
        System.out.println("S: Solving minefield...");

		openFirst();
		//drawOpenfield();

		while(gamePlay){

			//System.out.println("Solving minefield...");

			hintDivide();
			MineCheck();
			hintDivide();
			//hintCompre();
			openSquare();
			//drawOpenfield();
			playCheck();
		}

		long EndTime = System.currentTimeMillis();

		pack[0] = (EndTime - StartTime);

		if(gameClear) pack[1] = 0;
		else pack[1] = 1;

		pack[2] = burstCount;

		KeyValuePair<InetAddress, Integer> Maddress = null;
		try {
			Maddress = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Master),55878);
		} catch (UnknownHostException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		System.out.println("S: Sending -> M");
        StandardEnvelope enve = new StandardEnvelope(new AgentAddress(parentid), new StandardContentContainer(pack));
		MessageAPI.send(Maddress, enve);
		System.out.println("S: Sent -> M");
		System.out.println();
*/
	}

}