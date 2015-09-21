import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class KakakucomCrawler extends Thread {

	private JProgressBar bar;
	private JLabel lblState;
	private DefaultListModel<String> listModel;
	int state = 0;

	private String driver;
	private String server;
	private String dbname;
	private String url;
	private String user;
	private String password;
	private Connection con;

	public KakakucomCrawler(JProgressBar bar, JLabel lblState, DefaultListModel<String> listModel) {
		// TODO 自動生成されたコンストラクター・スタブ
		this.bar = bar;
		this.lblState = lblState;
		this.listModel = listModel;

		// JDBCドライバの登録
		driver = "org.postgresql.Driver";
		// データベースの指定
		server = "localhost";
		dbname = "db_kakakucom";
		url = "jdbc:postgresql://" + server + "/" + dbname;
		user = "keishiro";
		password = "wth050527";
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		try {
			con = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	public void run() {
		System.out.println("スレッドRun");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		getAllReview();
		
	}

	public void getAllReview() {
		int totalReviewCount = getTotalReviewCount();
		int i;
		if (getSavedReviewCount() > 0) {
			i = (int) Math.ceil(getSavedReviewCount() / 15.0);
		} else {
			i = 1;
		}

		while (true) {
			if (state == 0) {
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			} else {

				for (; i <= Math.ceil(totalReviewCount / 15.0); i++) {

					Elements elems;
					try {
						elems = Jsoup.connect("http://review.kakaku.com/review/newreview/PageNo=" + i + "/")
								.userAgent("Mozilla 5.0").followRedirects(true).timeout(0).get()
								.getElementsByClass("reviewBox");
						bar.setMaximum(totalReviewCount);
						for (Element elem : elems) {
							Statement stmt = con.createStatement();
							//System.out.println(elem.toString());						
							
							
							Boolean pState = false;
							do {
								if (duplicateCheck("review_tbl", "WHERE reviewid = '" + getReviewId(elem) + "'") == 0) {
									// 同じレビューがデータベースに無かった時の処理
									bar.setValue(getSavedReviewCount() + 1);
									listModel.add(0,
											new Date().toString() + "価格ドットコムよりレビュー取得中 - id：" + getReviewId(elem));
									System.out.println("reviewid：" + getReviewId(elem));
									String sql = "INSERT INTO review_tbl (entrydate,reviewid,rate,votes,customername,itemid) VALUES ('"
											+ toUnivFormat(getEntryDate(elem)) + "','" + getReviewId(elem) + "',"
											+ getRate(elem) + "," + getVotes(elem) + ",'" + getCusName(elem) + "','"
											+ getItemid(elem) + "');";
									int kekka = stmt.executeUpdate(sql);
									pState = true;
								} else {
									pState = true;
								}
							} while (pState == false);

							if (duplicateCheck("item_tbl", "WHERE itemid = '" + getItemid(elem) + "'") == 0) {
								// 新しい商品を見つけた時の処理
								listModel.add(0, new Date().toString() + "価格ドットコムより商品情報取得中 - id：" + getItemid(elem));
								System.out.println("itemid：" + getItemid(elem));
								String sql = "INSERT INTO item_tbl (itemid,itemjid,cat,maker,productname) VALUES ('"
										+ getItemid(elem) + "','" + getItemjid(elem) + "','" + getCat(elem) + "','"
										+ getMaker(elem) + "','" + getProductName(elem) + "');";
								int kekka = stmt.executeUpdate(sql);
							} else {
								pState = true;
							}

						}
					} catch (IOException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					} catch (SQLException e) {
						// TODO: handle exception
						e.printStackTrace();
					}

					if (state == 0) {break;}
					totalReviewCount = getTotalReviewCount();
				}
			}
		}
	}

	String toUnivFormat(String strDate) {
		return strDate.replaceAll("年|月", "-").replaceAll("日", "");
	}

	private String getEntryDate(Element elem) {
		String entrydate = elem.getElementsByClass("entryDate").first().text();
		Pattern p = Pattern.compile(".*(?=\\[)");
		Matcher m = p.matcher(entrydate);
		m.find();
		//System.out.println(m.group().trim());
		return m.group().replaceAll(" ", " ").trim() + ":00";
	}

	private String getReviewId(Element elem) {
		String reviewid = elem.getElementsByClass("entryDate").first().text();
		Pattern p = Pattern.compile("\\d+\\-\\d++");
		Matcher m = p.matcher(reviewid);
		m.find();
		return m.group().trim();
	}

	private int getRate(Element elem) {
		String rate = elem.getElementsByClass("total").first().getElementsByTag("td").first().text();
		return Integer.parseInt(rate);
	}

	private int getVotes(Element elem) {
		String votes = elem.getElementsByClass("referCount").first().getElementsByTag("span").first().text();
		return Integer.parseInt(votes);
	}

	private String getCusName(Element elem) {
		String cusName = elem.getElementsByClass("userName").first().getElementsByTag("a").first().text();
		return cusName.replaceAll("'", "''");
	}

	private String getItemid(Element elem) {
		String itemid = elem.getElementsByClass("prdimg").first().getElementsByTag("a").first().attr("href")
				.split("/")[4];
		return itemid;
	}

	private String getItemjid(Element elem) {
		// http://kakaku.com/item/J0000015447/ を/で区切った4つ目を取り出す
		try {
		String jid = elem.getElementsByClass("prdctgry").first().getElementsByTag("a").get(2).attr("href")
				.split("/")[4];

		// Jで始まっていなければ""を返す
		if (jid.charAt(0) == 'J') {
			return jid;
		} else {
			//System.out.println("jidありません1");
			return getItemid(elem);
		} 
		
		} catch (IndexOutOfBoundsException e) {
			//e.printStackTrace();
			//System.out.println("jidありません2");
			return getItemid(elem);
		}
		
	}

	private String getCat(Element elem) {

		String cat = elem.getElementsByClass("prdctgry").first().text().split(">")[0].trim();

		return cat.replaceAll("'", "''");
	}

	private String getMaker(Element elem) {
		String bc_of2 = elem.getElementsByClass("prdctgry").first().text().split(">")[1];
		return bc_of2.trim().replaceAll("'", "''");

	}

	private String getProductName(Element elem) {

		String[] breadcrumb = elem.getElementsByClass("prdctgry").first().text().split(">");
		String pname = "";
		if (breadcrumb.length == 3) {
			pname = breadcrumb[2].trim();
		} else if (breadcrumb.length == 4) {
			pname = breadcrumb[3].trim();
		} else {
			pname = breadcrumb[1].trim();
		}
		return pname.replaceAll("'", "''");
	}

	public int getTotalReviewCount() {
		boolean state = false;

		while (state == false) {
			Document doc;
			try {
				doc = Jsoup.connect("http://review.kakaku.com/review/newreview/").userAgent("Mozilla 5.0")
						.followRedirects(true).timeout(0).get();

				Element h3Area = doc.getElementsByClass("floatL").first();

				Pattern p = Pattern.compile("[0-9]+(?=件)");
				Matcher m = p.matcher(h3Area.text());
				m.find();
				state = true;
				return Integer.parseInt(m.group());
			} catch (IOException | NullPointerException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
		return 0;
	}

	public int getSavedReviewCount() {

		return tblDataCount("review_tbl");

	}

	private int tblDataCount(String tblName) {
		return duplicateCheck(tblName, "");
	}

	private int duplicateCheck(String tblName, String condition) {

		Statement stmt;
		try {
			stmt = con.createStatement();

			String sql = "SELECT COUNT(*) FROM " + tblName + " " + condition + ";";
			ResultSet rs = stmt.executeQuery(sql);

			rs.next();

			return rs.getInt(1);
		} catch (SQLException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
			// return duplicateCheck(tblName, condition);
			return 0;
		}
	}

	public synchronized void processStart() {
		// TODO 自動生成されたメソッド・スタブ
		state = 1;
		lblState.setText("データ収集中");
	}

	public synchronized void processPause() {
		// TODO 自動生成されたメソッド・スタブ
		state = 0;
		lblState.setText("処理中断中");
	}
}
