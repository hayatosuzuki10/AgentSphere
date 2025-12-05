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
public class MSSlave2old extends AbstractAgent implements IMessageListener {

	boolean gamePlay = true;
	boolean gameClear = true;
	boolean loopflag = true;
	int sendNum = 1;
	boolean sendflag = true;
	int loopCount;
	int mWidth = 900; // width of the minefield
	int mHeight = 480; // height of the minefield
	int mMines = 27000; // number of mines
	char[] mMinefield = new char[mHeight*mWidth]; // 2-dimensional array of MSBoards for our board
	int[] mineNum = new int[mHeight*mWidth];
	boolean[] openFlag = new boolean[mHeight*mWidth];
	boolean[] mineFlag = new boolean[mHeight*mWidth];
	List<Hint> HintList = new ArrayList<Hint>();
	int clearCount = 0;
	int playCount = 0;
	int burstCount = 0;
	int turnCount = 0;
	int shareCount = 0;

	long totalTime = 0;
	long tempTime1 = 0;
	long tempTime2 = 0;
	long CalculateTime = 0;
	long CommunicateTime = 0;

	int startpoint = 0;
	int slaveNum = 0;
	String[] ANlist = new String[3];

	long[] pack = new long[4];
	int[][] pack2_1 = new int[2][mHeight*mWidth+1];
	//int[][] pack2_2 = new int[3000][10];

	KeyValuePair<InetAddress, Integer> address = null;
	KeyValuePair<InetAddress, Integer> slave1 = null;
	KeyValuePair<InetAddress, Integer> slave2 = null;

	//String parentid;

    public void setattr (char[] minefield, int[] minenum, String[] ANlist, int i, int num){

    	this.mMinefield=minefield;
    	this.mineNum=minenum;
    	this.ANlist=ANlist;
    	this.startpoint=i;
    	this.slaveNum=num;

    }

    public MSSlave2old() {
    	super();
		// TODO Auto-generated constructor stub
	}


 //ここから
	public void run(){

		//KeyValuePair<InetAddress, Integer> address = null;
		//KeyValuePair<InetAddress, Integer> slave1 = null;
		//KeyValuePair<InetAddress, Integer> slave2 = null;

		//this.migrate();
		try {
			MessageAPI.registerMessageListener(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			address = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Master),55878);
			slave1 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave1),55878);
			slave2 = new KeyValuePair<InetAddress, Integer>(Inet4Address.getByName(IPAddress.Slave2),55878);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        //System.out.println("Enter the number of nodes in the graph");

        try{
            System.out.println("S"+slaveNum+": Waiting for master...");
           	Thread.sleep(3000); //3000ミリ秒Sleepする
        }catch(InterruptedException e){}

        //System.out.println();

        System.out.println("S"+slaveNum+": Solving minefield...");

		openFirst();
		//drawOpenfield();

		while(gamePlay){

			//System.out.println("Solving minefield...");

			turnCount++;

			hintDivide();
			MineCheck();
			hintDivide();
			//hintCompre();

			loopCount = 0;

			if(turnCount%10000 == 0) {
				while(loopflag) {
					loopCount++;
					if(sendflag) {
						BoardShare(slaveNum);
					}

					if(loopCount == 10000000){
						System.out.println("S"+slaveNum+": Waiting...");
					}
				}
			}

			loopflag = true;
			sendflag = true;

			openSquare();
			//drawOpenfield();
			playCheck();
		}

		if(gameClear) pack2_1[0][1] = 0;
		else pack2_1[0][1] = 1;

		pack2_1[0][2] = burstCount;

		pack2_1[0][0] = (int)(CommunicateTime*1000);

		pack2_1[0][mHeight*mWidth] = 0;

		System.out.println("S"+slaveNum+": Sending -> M");
        StandardEnvelope envelope = new StandardEnvelope(new AgentAddress(ANlist[0]), new StandardContentContainer(pack2_1));
        tempTime1 = System.currentTimeMillis();
		MessageAPI.send(address, envelope);
		tempTime2 = System.currentTimeMillis();
		System.out.println("S"+slaveNum+": Sent -> M");
		System.out.println();

		if(slaveNum == 1) {
			System.out.println("S1: result rep time   : " + ((tempTime2-tempTime1)/1000.0) + "[s]");
		}

		/*
		while(playCount != 500){
			  try{
				  Thread.sleep(10000);
		      }catch (InterruptedException e){
		      }
		}
		*/

	}
	//ここまで

	public class Hint {
		public List<Integer> hintSquare;
		public int mNum;

		public Hint(){
			hintSquare = new ArrayList<Integer>();
			mNum = 0;
		}
	}

	public void BoardShare(int n) {

		//int[][] pack2 = new int[HintList.size()+2][mHeight*mWidth];

		for(int i=0 ; i<mHeight*mWidth ; i++) {
			if(openFlag[i]) pack2_1[0][i] = 0;
			else            pack2_1[0][i] = 1;
			if(mineFlag[i]) pack2_1[1][i] = 0;
			else            pack2_1[1][i] = 1;
		}

		pack2_1[0][mHeight*mWidth] = slaveNum;

		//pack2_2[0][0] = HintList.size();
/*
		for(int i=0 ; i<HintList.size(); i++) {
			pack2_2[i+1][0] = HintList.get(i).mNum;
			pack2_2[i+1][1] = HintList.get(i).hintSquare.size();
			for(int j=0 ; j<pack2_2[i+1][1] ; j++) {
				pack2_2[i+1][j+2] = HintList.get(i).hintSquare.get(j);
			}
		}
*/
		//pack2 = openFlag, mineFlag, HintList.size, (mNum,hsSize,hs), (mNum,hsSize,hs), ...

		System.out.println("S"+slaveNum+": Sending -> M");
        StandardEnvelope envelope; // = new StandardEnvelope(new AgentAddress(IPAddress.Slave1), new StandardContentContainer(pack2));

        envelope = new StandardEnvelope(new AgentAddress(ANlist[0]), new StandardContentContainer(pack2_1));
        tempTime1 = System.currentTimeMillis();
        MessageAPI.send(address, envelope);
        tempTime2 = System.currentTimeMillis();

        CommunicateTime += (tempTime2-tempTime1);

		System.out.println("S"+slaveNum+": Sent -> M");
		System.out.println();

		sendflag = false;

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
		  openFlag[startpoint] = true;

		  if(mMinefield[startpoint] == '*'){
			  openFlag[startpoint] = false;
			  burstCount++;
			  mineFlag[startpoint] = true;
			  //hintCheck(num);
			  //gamePlay = false;
			  gameClear = false;
			  Hint h = new Hint();
			  h.hintSquare.add(startpoint);
			  h.mNum = 1;
			  HintList.add(h);
			  return;
		  }

		  HintList.add(hintSet(startpoint));
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

		  if(slaveNum == 1) {
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
		  }else if(slaveNum == 2) {
			  for(int j = mHeight*mWidth-1 ; j >= 0 ; j--){
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

		System.out.println("S"+slaveNum+": Received Message <- M");
		StandardContentContainer cont = (StandardContentContainer) envelope.getContent();
		pack2_1=(int[][]) cont.getContent();

		for(int i=0 ; i<mHeight*mWidth ; i++) {
			if(pack2_1[0][i] == 0) openFlag[i] = true;
			if(pack2_1[1][i] == 0) mineFlag[i] = true;
		}

		loopflag = false;

		System.out.println("S"+slaveNum+": Marge Board Complete...");
		//System.out.println("S"+slaveNum+": sendflag = "+sendflag);
		//System.out.println("S"+slaveNum+": sendNum = "+sendNum);
		//System.out.println("S"+slaveNum+": loopflag = "+loopflag);


	}

	public void receivedMessage(AbstractEnvelope envelope, int flag) {

	}
}






