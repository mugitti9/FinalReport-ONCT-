//中間凸包なし、短縮ありプログラム

import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;


//********* Change Classname, also in the main method **********

public class Partial_Liver_Resection_Dialog implements PlugInFilter {
	protected ImagePlus image;
	public static final int bc=-16777216;//0xff000000 is black color
	public static final int wc=-1;//0xffffffff is white color

	public double[] shutu = new double[9];//出力用parameter

	public boolean setujof=true;


	public double shoteny=1/5;//曲率
	public double shotenz=5/5;//曲率

	public int[][] ori;//すべてのピクセルをコピーする用

	//回転角
	public double kakux=60*(Math.PI)/180;
	public double kakuy=60*(Math.PI)/180;
	public double kakuz=0*(Math.PI)/180;
	//public static final double enhosei=0.01;
	//腫瘍中心からの最短距離
	public double dist=0;
	//
	public int[] point=new int[3];

	public static final int kakupos=10;//角度の精度
	public static final int shiftpos=5;//平行移動の精度
	public static final int shotenrate=50;//焦点の倍率


	public int setup(String arg, ImagePlus imp) {
		image = imp;
 		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		IJ.log("新しいやつ凸包化無し短縮あり");
		ImagePlus imp=IJ.getImage();
		ImageStack ist=imp.getStack();
		int slice=ist.getSize();
		int width =imp.getWidth();
		int height = imp.getHeight();

		ori=new int[slice][width*height];//初期化

		int shix=59;
		int shiy=75;
		int shiz=56;

		int cnt=0;//切除領域の体積（肝臓と緑の楕円体が重なった部分）
		int cntvol=0;//肝臓の体積
		//肝臓の腫瘍中心座標
		int jux=0;
		int juy=0;
		int juz=0;
		int cntshu=0;//肝臓の腫瘍の体積
		int min_vol=0;
		int check_break=0;

		ArrayList<Integer> setujo = new ArrayList<Integer>();

		//体積計算
		for(int i=1;i<=slice;i++){
			ImageProcessor ipa1=ist.getProcessor(i);

			int[] pix1=(int[])ipa1.getPixels();
			for(int pixnum=0;pixnum<pix1.length;pixnum++){
				//一次元配列の要素数からx,y座標を計算
				double enx=pixnum%width;
				double eny=(int)(pixnum/width);
				double enz=i;


				int col1=pix1[pixnum];
				//腫瘍の体積と中心座標を求める

				ori[i-1][pixnum]=col1;

				if(col1!=bc){
					cntvol++;
					if(col1==0xffff0000){
						jux+=enx;
						juy+=eny;
						juz+=enz;
						cntshu++;

						if(col1!=pix1[pixnum-1]||col1!=pix1[pixnum+1]||col1!=pix1[pixnum-width]||col1!=pix1[pixnum+width]){
							setujo.add((int)enx);
							setujo.add((int)eny);
							setujo.add((int)enz);
						}

					}
				}
			}
		}
		jux=jux/cntshu;
		juy=juy/cntshu;
		juz=juz/cntshu;
		IJ.log("Liver Volume"+cntvol);
		IJ.log("PIXELS: "+width*height*slice);
		IJ.log("Tumor Volume"+cntshu);
		IJ.log("Center x:"+jux+" y:"+juy+" z:"+juz);




		shutu[0]=shix;
		shutu[1]=shiy;
		shutu[2]=shiz;
		shutu[3]=kakux;
		shutu[4]=kakuy;
		shutu[5]=kakuz;
		shutu[6]=shoteny;
		shutu[7]=shotenz;
		shutu[8]=cntvol;
		min_vol=cntvol;

		double progress=0;


		for(shix=jux-50;shix<=jux+50;shix+=shiftpos){//shix=jux-50;shix<=jux+50;shix+=40
			for(shiy=juy-50;shiy<=juy+50;shiy+=shiftpos){//shiy=juy-50;shiy<=juy+50;shiy+=40
				for(shiz=juz-50;shiz<=juz+50;shiz+=shiftpos){//shiz=juz-50;shiz<=juz+50;shiz+=40
					for(int kakudox=0;kakudox<360;kakudox+=kakupos){
						kakux=kakudox*(Math.PI)/180;
						for(int kakudoy=0;kakudoy<360;kakudoy+=kakupos){
							kakuy=kakudoy*(Math.PI)/180;
							for(int kakudoz=0;kakudoz<360;kakudoz+=kakupos){
								kakuz=kakudoz*(Math.PI)/180;
								//腫瘍内部に頂点があるならキャンセル if
								if(ori[shiz][shix+shiy*width]==0xffff0000){
									setujof=false;
									progress+=9;
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
											double progsize=52488*64;
//											IJ.log("progress"+(progress/progsize));
											progress++;
											boolean houkatuf=true;
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
											if(houkatuf==true){
												int counter=0;
												for(int slicenum=0;slicenum<slice;slicenum++){
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
															}
														}
														}

													}


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
													min_vol=counter;
													check_break=0;
												}else {
													check_break=0;
												}
											}else{
												break;
											}


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














		/*



		//スタック画像を一枚ずつ読み込む
		for(int i=1;i<=slice;i++){
			ImageProcessor ipa1=ist.getProcessor(i);

			int[] pix1=(int[])ipa1.getPixels();


			//-*
			//肝臓の穴埋め
			int[] han=new int[pix1.length];
			Arrays.fill(han, 0);
			for(int ii=0;ii<pix1.length;ii++){
				if(pix1[ii]!=bc){
					pix1[ii]=wc;
				}
				if(ii<width||ii%width==0||ii>pix1.length-1-width||ii%(width-1)==0){
					han[ii]=1;
				}else if(pix1[ii]==bc&&(han[ii-1]==1||han[ii+1]==1||han[ii-width]==1||han[ii+width]==1)){
					han[ii]=1;
					if(pix1[ii-width]==bc&&han[ii-width]==0){
						ii-=(width+1);
					}else if(pix1[ii-1]==bc&&han[ii-1]==0){
						ii-=2;
					}
				}
			}


			//肝臓の体積計算、中心座標計算
			for(int ii=0;ii<pix1.length;ii++){
				if(han[ii]==0){
					pix1[ii]=wc;
					cntvol++;
					jux+=ii%width;
					juy+=(int)(ii/width);
					juz+=ii;
				}
			}

			//*-/

			for(int pixnum=0;pixnum<pix1.length;pixnum++){
				//一次元配列の要素数からx,y座標を計算
				double enx=pixnum%width;
				double eny=(int)(pixnum/width);
				double enz=i;


				int col1=pix1[pixnum];







				//------------------------------



				//内臓の表面を青にする
				if(((i==1)||(i==slice)||(pixnum<width)||(pixnum>=pix1.length-width)||(pixnum%width==0)||(pixnum%width==width-1))&&(col1==wc)){
					pix1[pixnum]=0x0000ff;
				}else if(col1==wc||col1==0xffff0000){
					ImageProcessor ipa2=ist.getProcessor(i+1);
					int[] pix2=(int[])ipa2.getPixels();
					int col2=pix2[pixnum];
					ImageProcessor ipa3=ist.getProcessor(i-1);
					int[] pix3=(int[])ipa3.getPixels();
					int col3=pix3[pixnum];
					if((pix1[pixnum-1]==bc)||(pix1[pixnum+1]==bc)||(pix1[pixnum-width]==bc)||(pix1[pixnum+width]==bc)||(col2==bc)||(col3==bc)){
						pix1[pixnum]=0x0000ff;
						if(dist==0){
							dist=Math.sqrt((jux-enx)*(jux-enx)+(juy-eny)*(juy-eny)+(juz-enz)*(juz-enz));
							point[0]=(int)enx;
							point[1]=(int)eny;
							point[2]=(int)enz;
						}
					}
				}

				//シフト
				enx-=shix;
				eny-=shiy;
				enz-=shiz;

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
					if(col1==wc||col1==0xffff0000){
						if(col1==0xffff0000){
							pix1[pixnum]+=0x00ff00;
						}else{
							pix1[pixnum]=0x00ff00;
						}
						cnt+=1;
					}else{
						pix1[pixnum]+=0x008000;
					}
				}


				//--------------






				//pix1[pixnum]=0xff0000;
				//ipa1.set((int)(pixnum%width), (int)(pixnum/width), (int)pix1[pixnum]);

			}

		}
		*/
		kakux=shutu[3]*(Math.PI)/180;
		kakuy=shutu[4]*(Math.PI)/180;
		kakuz=shutu[5]*(Math.PI)/180;

		shoteny=shutu[6]/shotenrate;
		shotenz=shutu[7]/shotenrate;

		for(int i=1;i<=slice;i++){
			ImageProcessor ipa1=ist.getProcessor(i);

			int[] pix1=(int[])ipa1.getPixels();
			for(int pixnum=0;pixnum<pix1.length;pixnum++){
				//一次元配列の要素数からx,y座標を計算
				double enx=pixnum%width;
				double eny=(int)(pixnum/width);
				double enz=i;


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
					if(col1==wc||col1==0xffff0000){
						if(col1==0xffff0000){
							pix1[pixnum]+=0x00ff00;
						}else{
							pix1[pixnum]=0x00ff00;
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
		IJ.log("shiftx :"+shutu[0]+"x2");//x2ついてる
		IJ.log("shifty :"+shutu[1]+"x2");
		IJ.log("shiftz :"+shutu[2]+"x2");
		IJ.log("kakudox :"+shutu[3]);
		IJ.log("kakudoy :"+shutu[4]);
		IJ.log("kakudoz :"+shutu[5]);
		IJ.log("shoteny :"+shutu[6]+"x2/"+shotenrate);
		IJ.log("shotenz :"+shutu[7]+"x2/"+shotenrate);
		IJ.log("cut volme :"+shutu[8]);
		imp.updateAndDraw();
	}
}
