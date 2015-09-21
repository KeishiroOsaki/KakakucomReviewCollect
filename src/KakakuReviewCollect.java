import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;


public class KakakuReviewCollect {

	private JFrame frame;
	private JButton btnStart;
	private JButton btnPause;
	JLabel lblState;
	JProgressBar bar;
	private JList<String> listProcess;
	private DefaultListModel<String> listModel;
	private KakakucomCrawler kakakucom;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					KakakuReviewCollect window = new KakakuReviewCollect();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public KakakuReviewCollect() {
		initialize();
		kakakucom = new KakakucomCrawler(bar,lblState,listModel);
		listProcess = new JList<String>(listModel);
		listProcess.setBounds(29, 94, 636, 334);
		frame.getContentPane().add(listProcess);
		kakakucom.start();
		System.out.println("ダイアログ初期化完了");
		//kakakucom.processStart();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("データ収集ツール");
		frame.setResizable(false);
		frame.setBounds(100, 100, 695, 469);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		frame.setIconImage(Toolkit.getDefaultToolkit().getImage("img/icon.png"));

		btnStart = new JButton("開始");
		btnStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				kakakucom.processStart();

			}
		});
		btnStart.setBounds(29, 19, 117, 29);
		frame.getContentPane().add(btnStart);

		btnPause = new JButton("中断");
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				kakakucom.processPause();
			}
		});
		btnPause.setBounds(158, 19, 117, 29);
		frame.getContentPane().add(btnPause);

		lblState = new JLabel("起動しました");
		lblState.setBounds(313, 24, 352, 16);
		frame.getContentPane().add(lblState);

		bar = new JProgressBar();
		bar.setBounds(29, 60, 636, 22);
		frame.getContentPane().add(bar);

		listModel = new DefaultListModel<String>();
	}
}
/*
public class KakakuReviewCollect {

	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ

	}

}*/
