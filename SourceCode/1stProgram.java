import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

//********* Change Classname, also in the main method **********

public class My_Plugin implements PlugInFilter {
	//小さい画像用(手法1のみ)
	static {
		System.loadLibrary("opencv_java400");
	}

	protected ImagePlus image;
	public static final int black=0xff000000;	//色の宣言
	public static final int white=0xffffffff;
	public static final int red=0xffff0000;
	public static final int lightgreen=0x00ff00;
	public static final int deepgreen=0x008000;

	public double[] shutu = new double[9];//出力用parameter

	public boolean setujof=true;


	public double shoteny=1/5;//曲率
	public double shotenz=5/5;//曲率

	public int[][][] ori;//すべてのピクセルをコピーする用

	//回転角
	public double kakux=60*(Math.PI)/180;
	public double kakuy=60*(Math.PI)/180;
	public double kakuz=0*(Math.PI)/180;
	//public static final double enhosei=0.01;
	//腫瘍中心からの最短距離
	public double dist=0;
	//
	public int[] point=new int[3];

	public static int kakupos=10;//角度の間隔
	public static int shiftpos=5;//平行移動の間隔
	public static int shotenrate=50;//焦点の倍率



	//Dialog
	boolean process_convex = true;
	boolean process_search = true;

	public int setup(String arg, ImagePlus imp) {
		image = imp;
 		return DOES_ALL;
	}

	boolean showdialog() {
		int[] wList = WindowManager.getIDList();
		if (wList==null) {
			error();
			return false;
		}
		String[] titles = new String[wList.length];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		GenericDialog gd = new GenericDialog("RegionGrowingSphere");
		gd.addChoice("OrgStack:", titles, titles[0]);
		gd.addNumericField("角度の精度:", kakupos, 0 );
		gd.addNumericField("平衡移動の精度:", shiftpos, 0 );
		gd.addNumericField("焦点の倍率:", shotenrate, 0 );
		gd.showDialog();
		if (gd.wasCanceled()) return false;

		int index1 = gd.getNextChoiceIndex();
		image = WindowManager.getImage(wList[index1]);
		kakupos = (int)gd.getNextNumber();
		shiftpos = (int)gd.getNextNumber();
		shotenrate = (int)gd.getNextNumber();
		IJ.log("凸包なし、すぐ抜けあり");
		IJ.log("初期条件");
		IJ.log("角度の精度は："+kakupos);
		IJ.log("平衡移動の精度は："+shiftpos);
		IJ.log("焦点の倍率は："+shotenrate);
		IJ.log(" ");
		return true;

	}

	void error() {	//エラー表示
		IJ.showMessage("RegionGrowingSphere", "This command requires one stack.\n");
	}

	public void run(ImageProcessor ip) {
		//IJ.log("aaaa");
		IJ.log("凸包なし飛び出しあり");
		if(!showdialog()) {
			return;
		}
		ImagePlus imp=IJ.getImage();
		ImageStack ist=imp.getStack();
		int slice=ist.getSize();
		int width =imp.getWidth();
		int height = imp.getHeight();

		//ImageStack ist1=imp1.getStack();


		ori=new int[slice][height][width];//初期化
		int[] vector=new int[3];

		int shix=59;
		int shiy=75;
		int shiz=56;
		int shiftx=0;
		int shifty=0;
		int shiftz=0;
		int shix_start=0;
		int shiy_start=0;
		int shiz_start=0;

		int cnt=0;//切除領域の体積（肝臓と緑の楕円体が重なった部分）
		int cntvol=0;//肝臓の体積
		//肝臓の腫瘍中心座標
		int jux=0;
		int juy=0;
		int juz=0;
		int jux_cal=0;
		int juy_cal=0;
		int juz_cal=0;
		double calx=0;
		double caly=0;
		double calz=0;
		double shux=0;
		double shuy=0;
		double shuz=0;
		int cntshu=0;//肝臓の腫瘍の体積
		double kakudox=0;
		double kakudoy=0;
		double kakudoz=0;

		int chotenx=25;
		int choteny=25;
		int chotenz=25;
		int shiftposx=shiftpos;
		int shiftposy=shiftpos;
		int shiftposz=shiftpos;

		int part=0;

		int kakuposx=kakupos;
		int kakuposy=kakupos;
		int kakuposz=kakupos;
		double kakudo=0;
		double kakudo_kankei=0;

		ArrayList<Double> setujox=new ArrayList<Double>();
		ArrayList<Double> setujoy=new ArrayList<Double>();
		ArrayList<Double> setujoz=new ArrayList<Double>();
		ArrayList<Integer> setujo=new ArrayList<Integer>();
		ArrayList<Integer> cal_vol = new ArrayList<Integer>();//体積計算用

		int point1=0;
		long start_time=System.currentTimeMillis();

		//体積計算
		for(int i=1;i<=slice;i++){
			ImageProcessor ipa=ist.getProcessor(i);

			int[] pix1=(int[])ipa.getPixels();
			for(int pixnum=0;pixnum<pix1.length;pixnum++){
				int col1=pix1[pixnum];
				int enx=pixnum%width;
				int eny=(int)(pixnum/width);
				int enz=i-1;//スタック画像の配列は1から始まるから

				//腫瘍の体積と中心座標を求める
				ori[i-1][eny][enx]=col1;

				if(col1!=black){
					cntvol++;
					jux_cal+=enx;
					juy_cal+=eny;
					juz_cal+=enz;
					if(col1==white) {//白のとき
						cal_vol.add(enx);
						cal_vol.add(eny);
						cal_vol.add(enz);
					}
					if(col1==red){//赤のとき
						jux+=enx;
						juy+=eny;
						juz+=enz;
						cntshu++;

						if(col1!=pix1[pixnum-1]||col1!=pix1[pixnum+1]||col1!=pix1[pixnum-width]||col1!=pix1[pixnum+width]){
							setujo.add((int)enx);
							setujo.add((int)eny);
							setujo.add((int)enz);
							point1++;
						}

					}
				}
			}
		}
		long end_time=System.currentTimeMillis();
		IJ.log("jurai houkatu POINT: "+point1);
		IJ.log("jurai houkatu TIME: "+(-(start_time - end_time))+"ms");



		jux=jux/cntshu;
		juy=juy/cntshu;
		juz=juz/cntshu;
		jux_cal=jux_cal/cntvol;
		juy_cal=juy_cal/cntvol;
		juz_cal=juz_cal/cntvol;
		IJ.log("Liver Volume"+cntvol);
		IJ.log("PIXELS: "+width*height*slice);
		IJ.log("Tumor Volume"+cntshu);
		IJ.log("腫瘍Center x:"+jux+" y:"+juy+" z:"+juz);
		IJ.log("肝臓Center x："+jux_cal+" y："+juy_cal+" z："+juz_cal);

		shutu[0]=shix;
		shutu[1]=shiy;
		shutu[2]=shiz;
		shutu[3]=kakux;
		shutu[4]=kakuy;
		shutu[5]=kakuz;
		shutu[6]=shoteny;
		shutu[7]=shotenz;
		shutu[8]=cntvol;

		double progress=0;
		long start_prog=System.currentTimeMillis();
		long end_prog=System.currentTimeMillis();
		long start_judge=System.currentTimeMillis();
		long end_judge=System.currentTimeMillis();
		long judgetime=0;
		long start_vol=System.currentTimeMillis();
		long end_vol=System.currentTimeMillis();
		long vol_time=0;
		int pixnum=0;

		//convex hull start
		for(int i=1;i<slice-1;i++){	//境界面取得
			for(int j=1;j<height-1;j++) {
				for(int k=1;k<width-1;k++) {
					pixnum=j*width+k;
					if((ori[i][j][k]==red)){
						setujo.add(k);
						setujo.add(j);
						setujo.add(i);
					}
				}
			}
		}
		//convex hull end

		IJ.log("肝臓の角度は x:"+calx+" y:"+caly+" z:"+calz);
		IJ.log("腫瘍の角度は x:"+shux+" y:"+shuy+" z:"+shuz);

		start_time=System.currentTimeMillis();
		long progsize = 3456649728L;

		for(shix=jux-50;shix<=jux+50;shix+=shiftpos){//shix=jux-50;shix<=jux+50;shix+=40	//この部分を追加することで従来手法となる
			for(shiy=juy-50;shiy<=juy+50;shiy+=shiftpos){//shiy=juy-50;shiy<=juy+50;shiy+=40
				for(shiz=juz-50;shiz<=juz+50;shiz+=shiftpos){//shiz=juz-50;shiz<=juz+50;shiz+=40
						for(kakudox=0;kakudox<180;kakudox+=kakupos){
							kakux=kakudox*(Math.PI)/180;
							for(kakudoy=0;kakudoy<360;kakudoy+=kakupos){
								kakuy=kakudoy*(Math.PI)/180;
								for(kakudoz=0;kakudoz<360;kakudoz+=kakupos){
									kakuz=kakudoz*(Math.PI)/180;

								if(kakudox==20&&kakudoy==160&&kakudoz==240&&shix==123&&shiy==171&&shiz==31) {
									IJ.log("該当の値に来ました");
								}

								//腫瘍内部に頂点があるならキャンセル if
								if(ori[shiz][shiy][shix]==red){
									setujof=false;
									progress+=16;
								}else{
									setujof=true;
								}

								if(setujof==true){
									for(int shoy=1;shoy<=10;shoy*=2){//shoteny=1/100;shoteny<=1/10;shoteny+=4/100
										//IJ.log("shoteny"+shoteny);
										shoteny=(double)shoy/shotenrate;
										for(int shoz=1;shoz<=10;shoz*=2){//shotenz=1/100;shotenz<=1/10;shotenz+=4/100
											//shoten放物面が大きい順から
											shotenz=(double)shoz/shotenrate;
											//IJ.log("shotenz"+shotenz);

											//IJ.log("progress"+(progress/progsize));
											progress++;
											boolean houkatuf=true;

											start_judge=System.currentTimeMillis();

											for(int houkatu=0;houkatu<setujo.size();houkatu+=3){
												double enx=setujo.get(houkatu);
												double eny=setujo.get(houkatu+1);
												double enz=setujo.get(houkatu+2);
												enx-=shix;
												eny-=shiy;
												enz-=shiz;
												double tranx=enx;
												double trany=eny;
												double tranz=enz;

												enx=tranx*Math.cos(kakuz)*Math.cos(kakuy)+trany*(-Math.cos(kakux)*Math.sin(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.cos(kakuz))+tranz*(Math.sin(kakux)*Math.sin(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.cos(kakuz));
												eny=tranx*Math.cos(kakuy)*Math.sin(kakuz)+trany*(Math.cos(kakux)*Math.cos(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.sin(kakuz))+tranz*(-Math.sin(kakux)*Math.cos(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.sin(kakuz));
												enz=-tranx*Math.sin(kakuy)+trany*Math.sin(kakux)*Math.cos(kakuy)+tranz*Math.cos(kakux)*Math.cos(kakuy);

												if((double)(-(eny*eny*shoteny+enz*enz*shotenz))<enx){
													houkatuf=false;
													//IJ.log("break");
													break;
												}

											}

											end_judge=System.currentTimeMillis();
											judgetime+=(end_judge - start_judge);

											start_vol=System.currentTimeMillis();

											if(houkatuf==true){
												int counter=0;
												boolean cntfrag=true;//切除体積が暫定解より大きい:false

												//体積計算を白の領域のみに行う
												for(int cal_vol_num=0; cal_vol_num<cal_vol.size();cal_vol_num+=3){

													double enx=(double)cal_vol.get(cal_vol_num);
													double eny=(double)cal_vol.get(cal_vol_num+1);
													double enz=(double)cal_vol.get(cal_vol_num+2);
													enx-=shix;
													eny-=shiy;
													enz-=shiz;
													double tranx=enx;
													double trany=eny;
													double tranz=enz;

													enx=tranx*Math.cos(kakuz)*Math.cos(kakuy)+trany*(-Math.cos(kakux)*Math.sin(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.cos(kakuz))+tranz*(Math.sin(kakux)*Math.sin(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.cos(kakuz));
													eny=tranx*Math.cos(kakuy)*Math.sin(kakuz)+trany*(Math.cos(kakux)*Math.cos(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.sin(kakuz))+tranz*(-Math.sin(kakux)*Math.cos(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.sin(kakuz));
													enz=-tranx*Math.sin(kakuy)+trany*Math.sin(kakux)*Math.cos(kakuy)+tranz*Math.cos(kakux)*Math.cos(kakuy);

													if((double)(-(eny*eny*shoteny+enz*enz*shotenz))>=(enx)){
														counter++;
														if(counter>shutu[8]) {
															cntfrag=false;
														}
													}

													if(!cntfrag) {
														break;
													}

											}

												//体積計算短縮させないやつ
												/*
												 * for(int slicenum=0;slicenum<slice;slicenum++){
													for(int pixelnum=0;pixelnum<width*height;pixelnum++){
														double enx=pixelnum%width;
														double eny=(int)(pixelnum/width);
														double enz=slicenum;
														enx-=shix;
														eny-=shiy;
														enz-=shiz;
														double tranx=enx;
														double trany=eny;
														double tranz=enz;

														enx=tranx*Math.cos(kakuz)*Math.cos(kakuy)+trany*(-Math.cos(kakux)*Math.sin(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.cos(kakuz))+tranz*(Math.sin(kakux)*Math.sin(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.cos(kakuz));
														eny=tranx*Math.cos(kakuy)*Math.sin(kakuz)+trany*(Math.cos(kakux)*Math.cos(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.sin(kakuz))+tranz*(-Math.sin(kakux)*Math.cos(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.sin(kakuz));
														enz=-tranx*Math.sin(kakuy)+trany*Math.sin(kakux)*Math.cos(kakuy)+tranz*Math.cos(kakux)*Math.cos(kakuy);

														if((double)(-(eny*eny*shoteny+enz*enz*shotenz))>=(enx)){
															if(ori[slicenum][pixelnum]!=0xff000000){
																counter++;
																if(counter>shutu[8]) {
																	cntfrag=false;
																}
															}
														}

														if(!cntfrag) {
															break;
														}
													}
													if(!cntfrag) {
														break;
													}
												}
												 */

												/*


												 */









												//IJ.log("couter"+counter);
												if(counter<shutu[8]){
													shutu[0]=shix;
													shutu[1]=shiy;
													shutu[2]=shiz;
													shutu[3]=kakudox;
													shutu[4]=kakudoy;
													shutu[5]=kakudoz;
													shutu[6]=shoy;
													shutu[7]=shoz;
													shutu[8]=counter;
													IJ.log("progress:"+(progress/progsize));
													end_prog=System.currentTimeMillis();
													IJ.log("between time:"+(end_prog - start_prog));
													start_prog=System.currentTimeMillis();
													IJ.log("shiftx :"+shutu[0]);//x2ついてる
													IJ.log("shifty :"+shutu[1]);
													IJ.log("shiftz :"+shutu[2]);
													IJ.log("kakudox :"+shutu[3]);
													IJ.log("kakudoy :"+shutu[4]);
													IJ.log("kakudoz :"+shutu[5]);

												}
											}else{
												break;
											}
											end_vol=System.currentTimeMillis();
											vol_time+=(end_vol - start_vol);


										}
									}
								}

								//
							}
						}
					}
				}
			}
		}


		kakux=shutu[3]*(Math.PI)/180;
		kakuy=shutu[4]*(Math.PI)/180;
		kakuz=shutu[5]*(Math.PI)/180;

		shoteny=shutu[6]/shotenrate;
		shotenz=shutu[7]/shotenrate;

		for(int i=1;i<=slice;i++){
			ImageProcessor ipa1=ist.getProcessor(i);

			int[] pix1=(int[])ipa1.getPixels();
			for(pixnum=0;pixnum<pix1.length;pixnum++){
				//一次元配列の要素数からx,y座標を計算
				double enx=pixnum%width;
				double eny=(int)(pixnum/width);
				double enz=i-1;
				int conx=pixnum%width;
				int cony=(int)(pixnum/width);
				int conz=i-1;

				int col1=pix1[pixnum];
				//シフト
				enx-=shutu[0];
				eny-=shutu[1];
				enz-=shutu[2];

				double tranx=enx;
				double trany=eny;
				double tranz=enz;
				//同時にやるな!!!
				//enx=enx*Math.cos(kakuy)+enz*Math.sin(kakuy);
				//eny=eny;
				//enz=-(enx*Math.sin(kakuy))+enz*Math.cos(kakuy);


				//ロールピッチヨーで回転
				enx=tranx*Math.cos(kakuz)*Math.cos(kakuy)+trany*(-Math.cos(kakux)*Math.sin(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.cos(kakuz))+tranz*(Math.sin(kakux)*Math.sin(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.cos(kakuz));
				eny=tranx*Math.cos(kakuy)*Math.sin(kakuz)+trany*(Math.cos(kakux)*Math.cos(kakuz)+Math.sin(kakux)*Math.sin(kakuy)*Math.sin(kakuz))+tranz*(-Math.sin(kakux)*Math.cos(kakuz)+Math.cos(kakux)*Math.sin(kakuy)*Math.sin(kakuz));
				enz=-tranx*Math.sin(kakuy)+trany*Math.sin(kakux)*Math.cos(kakuy)+tranz*Math.cos(kakux)*Math.cos(kakuy);




				//放物線の回転体を作成する
				if((double)(-(eny*eny*shoteny+enz*enz*shotenz))>=(enx)){
					if(col1!=black){
						if(col1==0xffff0000){
							pix1[pixnum]+=0x00ff00;
						}else if(col1==white){
							pix1[pixnum]=0x00ff00;
						}else {
							IJ.log("NOT white or red position");
							IJ.log("x:"+conx+" y:"+cony+" z:"+conz);
						}
						cnt+=1;
					}else{
						pix1[pixnum]+=0x008000;
					}
				}


				//--------------

				//pix1[pixnum]=0xff0000;
				ipa1.set((int)(pixnum%width), (int)(pixnum/width), (int)pix1[pixnum]);
			}
		}

		//IJ.log("CUT Volume: "+cnt);
		IJ.log("shiftx :"+shutu[0]);//x2ついてる
		IJ.log("shifty :"+shutu[1]);
		IJ.log("shiftz :"+shutu[2]);
		IJ.log("kakudox :"+shutu[3]);
		IJ.log("kakudoy :"+shutu[4]);
		IJ.log("kakudoz :"+shutu[5]);
		IJ.log("shoteny :"+shutu[6]+"/"+shotenrate);
		IJ.log("shotenz :"+shutu[7]+"/"+shotenrate);
		IJ.log("cut volme :"+(shutu[8]+(double)cntshu));
		IJ.log("cnt :"+cnt);
		end_time=System.currentTimeMillis();
		IJ.log("search time:"+(end_time - start_time)+"ms");
		IJ.log("judge time:"+judgetime+"ms");
		IJ.log("Calculation time"+vol_time+"ms");

		imp.updateAndDraw();
	}
}
