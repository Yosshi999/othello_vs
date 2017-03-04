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
    SettingPanel right = new SettingPanel("Black", Color.BLACK);
    GamePanel center = new GamePanel(WIN_WIDTH/2, WIN_HEIGHT/2, 400, left, right);

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
  SettingPanel white, black;
  JTextArea kifu;

  GamePanel(int x, int y, int size, SettingPanel _white, SettingPanel _black) {
    //setLayout( new BorderLayout() );
    white = _white;
    black = _black;

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

    kifu = new JTextArea("kifu", 7, 40);
    kifu.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    JScrollPane kifusc = new JScrollPane(kifu);
    add(kifusc, BorderLayout.SOUTH);

    //add(new JPanel(), BorderLayout.WEST);
    //add(new JPanel(), BorderLayout.EAST);
  }

  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals("Next")) {
      changeScene("startGame");
    }
  }

  public void changeScene(String query) {
    if (query.equals("startGame")) {
      layout.last(screen);
      board.init();
      repaint();
      board.startGame(white, black, kifu);
    }
    if (query.equals("Standby")) {
      layout.first(screen);
    }
  }
}
class Board extends JPanel implements ActionListener {
  JTextArea kifu;
  Cell[][] cells = new Cell[8][8];
  SettingPanel[] player = new SettingPanel[2];
  boolean[] isHuman = new boolean[2];
  long[] leftTime = new long[2];
  int turn; // 0: white, 1: black
  boolean running;
  long startThinking;
  Timer loopTimer;

  Board() {
    setLayout( new GridLayout(8,8) );
    for (int i=0; i<8; i++) {
      for (int j=0; j<8; j++) {
        Cell cell = new Cell(i, j, this);
        cell.setBackground(Color.GREEN);
        cell.setBorder(new EtchedBorder(EtchedBorder.RAISED));
        cells[i][j] = cell;
        add(cell);
      }
    }
  }

  boolean validateAndFlip(int row, int line, boolean simulation) {
    if (cells[row][line].getState() != 0) return false;
    int[][] offset = {{0,1}, {1,0}, {0,-1}, {-1,0}, {1,1}, {-1,-1}, {1,-1}, {-1,1}};


    // white turn=0,stone=1 : black turn=1,stone=-1
    int myColor = turn==0?1:-1;
    boolean canFlip = false;
    for (int i=0; i<offset.length; i++) {
      boolean hasEnemy = false;
      for (int r=row, l=line; 0<=r && r<8 && 0<=l && l<8; r+=offset[i][0], l+=offset[i][1]) {
        if (r==row && l==line) continue; // first ite
        int state = cells[r][l].getState();
        if (state == 0) break;  // unable to flip
        if (state == myColor*(-1)) {
          hasEnemy = true;
          continue;
        }
        if (state == myColor) {
          if (hasEnemy) {
            // canFlip
            canFlip = true;
            if (!simulation) {
              int back_r = r, back_l = l;
              while (back_r != row || back_l != line) {
                cells[back_r][back_l].putStone(myColor);
                back_r -= offset[i][0];
                back_l -= offset[i][1];
              }
            }
          } else {
            // unable to Flip
            break;
          }
        }
      }
    }
    if (canFlip) {
      if (!simulation) {
        cells[row][line].putStone(turn==0?1:-1);
        kifu.append(""+"abcdefgh".charAt(line) + (row+1) + (turn==0?" ":"\n"));

        // update the number of stones
        int w = 0, b = 0;
        for (int i=0; i<8; i++) {
          for (int j=0; j<8; j++) {
            int state = cells[i][j].getState();
            if (state == 1) w++;
            if (state == -1) b++;
          }
        }
        player[0].setStones(w);
        player[1].setStones(b);
      }
      return true;
    } else {
      return false;
    }
  }
  public void onClick(int row, int line) {
    if (running && isHuman[turn]) { // if waiting for human player
      // validate & FlipStone
      boolean canFlip = validateAndFlip(row, line, false);
      if (canFlip) {
        switchTurn(0);
      }
    }
  }

  void switchTurn(int passCount) {
    if (passCount == 0) { // if this func isn't called after pass
      // stop timer
      long passed = System.currentTimeMillis() - startThinking;
      leftTime[turn] -= passed;
    }
    if (passCount == 2) { // both players pass
      // game over
      running = false;
      return;
    }

    // switch turn
    turn ^= 1;
    // pass or not
    boolean canPut = false;
    for (int i=0; i<8; i++) {
      for (int j=0; j<8; j++) {
        if (validateAndFlip(i,j,true)) {
          canPut = true;
          break;
        }
      }
      if (canPut) break;
    }
    if (!canPut) {
      // pass
      kifu.append("--" + (turn==0?" ":"\n"));
      switchTurn(passCount+1);
      return;
    }

    startThinking = System.currentTimeMillis();
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

  public void startGame(SettingPanel white, SettingPanel black, JTextArea _kifu) {
    player[0] = white;
    player[1] = black;
    kifu = _kifu;

    for (int i=0; i<2; i++) {
      player[i].setStones(2);
      isHuman[i] = player[i].isHuman();
      if (!isHuman[i]) {
        //exec

      }
      leftTime[i] = player[i].getTime()*1000;
      player[i].setLeftTime(leftTime[i]);
    }

    turn = 0;
    running = true;
    startThinking = System.currentTimeMillis();
    kifu.setText("--begin--\n");

    loopTimer = new Timer(30, this);
    loopTimer.start();
  }

  public void actionPerformed(ActionEvent e) {
    if (!running) {
      loopTimer.stop();
      return;
    }
    if (isHuman[turn]) {  // human
      long passed = System.currentTimeMillis() - startThinking;
      if (leftTime[turn] - passed <= 0) {
        player[turn].setLeftTime(0);
        // TLE
        running = false;
      } else {
        player[turn].setLeftTime(leftTime[turn] - passed);
      }
    }
  }

}
class Cell extends JPanel implements MouseListener {
  int pos_row, pos_line, r;
  Board mgr;

  int state;  // 0: empty, -1: black, 1: white
  Cell(int row, int line, Board board) {
    pos_row = row;
    pos_line = line;
    r = 40;
    state = 0;
    mgr = board;
    addMouseListener(this);
  }

  public void mouseClicked(MouseEvent e) {
    mgr.onClick(pos_row, pos_line);
  }

  public void putStone(int stoneCol) {
    state = stoneCol;
    repaint();
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

  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
}


class SettingPanel extends JPanel {
  PlayerOption op;
  JLabel stones;
  TimeOption ti;
  JLabel timer;
  JTextArea cout, cerr;

  SettingPanel(String name, Color color) {
    //setLayout( new BoxLayout(this, BoxLayout.Y_AXIS) );
    setLayout( new GridLayout(7,1) );

    JLabel jl = new JLabel(name);
    jl.setForeground(color);
    jl.setFont(new Font(null, 0, 30));
    add(jl);

    op = new PlayerOption();
    add(op);

    stones = new JLabel("0");
    stones.setFont( new Font(null, 0, 30));
    add(stones);

    ti = new TimeOption();
    add(ti);

    timer = new JLabel("0:00");
    timer.setFont( new Font(null, 0, 30) );
    add(timer);

    cout = new JTextArea("cout");
    cout.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    JScrollPane coutsc = new JScrollPane(cout);
    add(coutsc);
    cerr = new JTextArea("cerr");
    cerr.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    JScrollPane cerrsc = new JScrollPane(cerr);
    add(cerrsc);
  }

  public boolean isHuman() {
    return op.isHuman();
  }
  public String getPath(){
    return op.getPath();
  }
  public void setStones(int num) {
    stones.setText(String.valueOf(num));
  }
  public void setLeftTime(long millisec) {
    timer.setText( String.format("%d:%02d.%03d", millisec/1000/60, millisec/1000%60, millisec%1000) );
  }
  public int getTime() {
    return ti.getTime();
  }
}

class TimeOption extends JPanel {
  JSpinner time;

  TimeOption() {
    setLayout(null);
    JLabel jl = new JLabel("time:");
    jl.setBounds(10,10,30,20);
    add(jl);

    time = new JSpinner();
    time.setBounds(50,10,60,20);
    time.setValue(180);
    add(time);

    JLabel jl2 = new JLabel("sec");
    jl2.setBounds(120,10,30,20);
    add(jl2);
  }

  public int getTime() {  // return seconds
    return (Integer)time.getValue();
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

  public boolean isHuman() {
    return radio1.isSelected();
  }
  public String getPath() {
    return text.getText();
  }
}
