import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.EtchedBorder;

public class othello extends JFrame {

  static final int WIN_HEIGHT = 600;
  static final int WIN_WIDTH = 900;

  othello() {
    setBounds( 20, 30, WIN_WIDTH, WIN_HEIGHT );
    setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
    setTitle("othello");


    SettingPanel left = new SettingPanel("White", Color.WHITE);
    GamePanel center = new GamePanel(WIN_WIDTH/2, WIN_HEIGHT/2, 400);
    SettingPanel right = new SettingPanel("Black", Color.BLACK);
    left.setPreferredSize(new Dimension(180, WIN_HEIGHT));
    //center.setPreferredSize(new Dimension(320, WIN_HEIGHT));
    right.setPreferredSize(new Dimension(180, WIN_HEIGHT));

    add(left, BorderLayout.WEST);
    add(center, BorderLayout.CENTER);
    add(right, BorderLayout.EAST);

  }

  public static void main(String[] args) {
    // make window frame
    othello mainframe = new othello();
    mainframe.setVisible(true);

  }
}
class GamePanel extends JPanel implements ActionListener {
  JPanel screen;
  CardLayout layout;
  Board board;

  GamePanel(int x, int y, int size) {
    //setLayout( new BorderLayout() );

    board = new Board();
    board.setPreferredSize(new Dimension(size,size));
    //add(board, BorderLayout.NORTH);

    JPanel readyBoard = new JPanel();
    readyBoard.setPreferredSize(new Dimension(size, size));
    readyBoard.setBackground(Color.GREEN);
    JButton startButton = new JButton("start");
    readyBoard.add(startButton, BorderLayout.CENTER);
    startButton.addActionListener(this);
    startButton.setActionCommand("Next");

    layout = new CardLayout();
    screen = new JPanel(layout);
    screen.add(readyBoard, "ready");
    screen.add(board, "game");
    add(screen, BorderLayout.NORTH);

    JTextArea kifu = new JTextArea("kifu", 7, 40);
    kifu.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    JScrollPane kifusc = new JScrollPane(kifu);
    add(kifusc, BorderLayout.SOUTH);

    //add(new JPanel(), BorderLayout.WEST);
    //add(new JPanel(), BorderLayout.EAST);
  }

  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals("Next")) {
      layout.next(screen);
      board.init();
      board.startGame();
    }
  }
}
class Board extends JPanel {
  Cell[][] cells = new Cell[8][8];
  Board() {
    setLayout( new GridLayout(8,8) );
    for (int i=0; i<8; i++) {
      for (int j=0; j<8; j++) {
        Cell cell = new Cell(i, j);
        cell.setBackground(Color.GREEN);
        cell.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        cells[i][j] = cell;
        add(cell);
      }
    }
  }

  public void init() {
    for (int i=0; i<8; i++) {
      for (int j=0; j<8; j++) {
        cells[i][j].putStone(0);
      }
    }
    cells[3][3].putStone(1);
    cells[4][4].putStone(1);
    cells[3][4].putStone(-1);
    cells[4][3].putStone(-1);
  }

}
class Cell extends JPanel {
  int pos_row, pos_line, r;

  int state;  // 0: empty, -1: black, 1: white
  Cell(int row, int line) {
    pos_row = row;
    pos_line = line;
    r = 40;
    state = 0;
  }

  public void putStone(int stoneCol) {
    state = stoneCol;
  }
  public int getState() {
    return state;
  }
  public void FlipStone() {
    state *= -1;
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (state == 0) return;
    float cellsize = getSize().width;
    switch (state) {
      case -1:
      g.setColor(Color.BLACK);
      g.fillOval((int)cellsize/2-r/2, (int)cellsize/2-r/2, r, r);
      g.setColor(Color.WHITE);
      g.drawOval((int)cellsize/2-r/2, (int)cellsize/2-r/2, r, r);
      break;

      case 1:
      g.setColor(Color.WHITE);
      g.fillOval((int)cellsize/2-r/2, (int)cellsize/2-r/2, r, r);
      g.setColor(Color.BLACK);
      g.drawOval((int)cellsize/2-r/2, (int)cellsize/2-r/2, r, r);
    }
  }
}


class SettingPanel extends JPanel {
  SettingPanel(String name, Color color) {
    //setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
    setLayout( new GridLayout(7,1) );

    JLabel jl = new JLabel(name);
    jl.setForeground(color);
    jl.setFont(new Font(null, 0, 30));
    add(jl);

    PlayerOption op = new PlayerOption();
    add(op);

    JLabel stones = new JLabel("0");
    stones.setFont( new Font(null, 0, 30));
    add(stones);

    TimeOption ti = new TimeOption();
    add(ti);

    JLabel timer = new JLabel("0:00");
    timer.setFont( new Font(null, 0, 30) );
    add(timer);

    JTextArea cout = new JTextArea("cout");
    cout.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    JScrollPane coutsc = new JScrollPane(cout);
    add(coutsc);
    JTextArea cerr = new JTextArea("cerr");
    cerr.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    JScrollPane cerrsc = new JScrollPane(cerr);
    add(cerrsc);
  }
}

class TimeOption extends JPanel {
  TimeOption() {
    setLayout(null);
    JLabel jl = new JLabel("time:");
    jl.setBounds(10,10,30,20);
    add(jl);

    JSpinner time = new JSpinner();
    time.setBounds(50,10,60,20);
    add(time);

    JLabel jl2 = new JLabel("sec");
    jl2.setBounds(120,10,30,20);
    add(jl2);

  }
}
class PlayerOption extends JPanel implements ChangeListener {
  JRadioButton radio1, radio2;
  JTextField text;

  PlayerOption() {
    //setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
    //setLayout( null );
    setLayout( new GridLayout(3,1) );
    radio1 = new JRadioButton("human");
    radio2 = new JRadioButton("com");
    radio2.setSelected(true);

    ButtonGroup group = new ButtonGroup();
    group.add(radio1);
    group.add(radio2);
    radio1.addChangeListener(this);
    radio2.addChangeListener(this);

    JPanel text_wrap = new JPanel(null);

    JLabel jl = new JLabel("path:");
    jl.setBounds(0,0,40,20);
    text_wrap.add(jl);

    text = new JTextField();
    text.setBounds(40,0,130,20);
    text_wrap.add(text);

    add(radio1);
    add(radio2);
    add(text_wrap);
  }

  public void stateChanged(ChangeEvent e) {
    if (radio1.isSelected()) {
      text.setEditable(false);
    }
    if (radio2.isSelected()) {
      text.setEditable(true);
    }
  }
}
