import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.EtchedBorder;

import java.io.*;
import java.lang.Exception.*;

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

    addWindowListener(new WindowAdapter() {
              public void windowClosing(WindowEvent e)  {
                  center.force_halt();
                  System.exit(0);
              }
         });

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
    add(screen, BorderLayout.CENTER);

    kifu = new JTextArea("kifu", 7, 40);
    kifu.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    JScrollPane kifusc = new JScrollPane(kifu);
    add(kifusc, BorderLayout.SOUTH);

    JButton stopButton = new JButton("stop");
    stopButton.addActionListener(this);
    stopButton.setActionCommand("Stop");
    add(stopButton);

    //add(new JPanel(), BorderLayout.WEST);
    //add(new JPanel(), BorderLayout.EAST);
  }
  public void force_halt() {
    // on closing window
    board.destroy_all();
  }

  public void actionPerformed(ActionEvent e) {
    String cmd = e.getActionCommand();
    if (cmd.equals("Next")) {
      changeScene("startGame");
    }
    if (cmd.equals("Stop")) {
      board.halt();
      changeScene("Standby");
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
class InputThread extends Thread {
  BufferedReader br;
  Board mgr;
  int id;
  InputThread(InputStream is, Board caller, int myId) {
    br = new BufferedReader(new InputStreamReader(is));
    mgr = caller;
    id = myId;
    System.out.println("open errthread " + id);
  }
  @Override
  public void run() {
    try {
      for (;;) {
        String line = br.readLine();
        if (line == null) break;
        mgr.onCerr(line, id);
      }
    } catch (IOException e) {
      System.out.println("exception");
      mgr.halt();
      throw new RuntimeException(e);
    }
    System.out.println("close errthread " + id);
  }
}

class IOThread extends Thread {
  BufferedReader br;
  BufferedWriter bw;
  Board mgr;
  int id;
  IOThread(InputStream is, OutputStream os, Board caller, int myId) {
    br = new BufferedReader(new InputStreamReader(is));
    bw = new BufferedWriter(new OutputStreamWriter(os));
    mgr = caller;
    id = myId;
    System.out.println("open iothread " + id);
  }
  public void write(long nanosec, int lastRow, int lastLine, String[] board) {
    try {
      // System.out.println("writing");
      long leftTime = nanosec/1000000; // millisec
      bw.write("" + leftTime);
      bw.newLine();
      bw.flush();
      bw.write( String.format("%d %d", lastRow, lastLine) );
      bw.newLine();
      bw.flush();
      for (int i=0; i<board.length; i++) {
        bw.write(board[i]);
        bw.newLine();
        bw.flush();
      }
      // System.out.println("wrote");
    } catch (IOException e) {
      e.printStackTrace();
      mgr.halt();
    }
  }

  @Override
  public void run() {
    try {
      for (;;) {
        String line = br.readLine();
        if (line == null) break;
        mgr.onCout(line, id);
      }
    } catch (IOException e) {
      System.out.println("exception");
      mgr.halt();
      throw new RuntimeException(e);
    }
    System.out.println("close iothread " + id);
  }
}
class Board extends JPanel implements ActionListener {
  JTextArea kifu;
  Cell[][] cells = new Cell[8][8];
  SettingPanel[] player = new SettingPanel[2];
  boolean[] isHuman = new boolean[2];
  Process[] process = new Process[2];
  IOThread[] iot = new IOThread[2];
  InputThread[] it = new InputThread[2];

  long[] leftTime = new long[2];  // nano seconds
  int turn; // 0: white, 1: black
  boolean running;
  boolean waiting;
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

  public void halt() {
    System.out.println("halt");
    running = false;
  }

  public void destroy_all() {
    // on window closing
    for (int i=0; i<2; i++) {
      if (!isHuman[i] && process[i]!=null) {
        // try {
        //   process[i].getErrorStream().close();
        //   process[i].getInputStream().close();
        //   process[i].getOutputStream().close();
        // } catch (IOException e) {
        //   e.printStackTrace();
        // }
        process[i].destroy();
        process[i] = null;
        System.out.println("destroyed " + i);

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
    if (running && waiting && isHuman[turn]) { // if waiting for human player
      // validate & FlipStone
      boolean canFlip = validateAndFlip(row, line, false);
      if (canFlip) {
        endTurn();
      }
    }
  }

  public void onCerr(String input, int id) {
    if (id == 0 || id == 1) {
      player[id].Cerr(input);
    }
  }

  public void onCout(String input, int id) {
    if (id == 0 || id == 1) {
      player[id].Cout(input);
    }
    try {
      if (running && waiting && !isHuman[turn]) { // if waiting for AI
        int row,line;
        String[] sp = input.split(" ", 0);
        row = Integer.parseInt(sp[0]);
        line = Integer.parseInt(sp[1]);

        boolean canFlip = validateAndFlip(row, line, false);
        if (canFlip) {
          endTurn();
        } else {
          // wrong answer
          // halt
          kifu.append("##unable to put##\n");
          halt();
        }
      }
    } catch (NumberFormatException e) {
      kifu.append("##bad format##\n");
      halt();
    }
  }

  void endTurn() {
    waiting = false;
    // stop timer
    long passed = System.nanoTime() - startThinking;
    leftTime[turn] -= passed;
    player[turn].setLeftTime(leftTime[turn]);
    ActionListener task = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        switchTurn(0);
      }
    };
    Timer t = new Timer(100, task);
    t.setRepeats(false);
    t.start();
  }

  void switchTurn(int passCount) {
    if (passCount == 0) { // if this func isn't called after pass
      // nothing
    }
    if (passCount == 2) { // both players pass
      // game over
      halt();
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

    startThinking = System.nanoTime();
    waiting = true;
    if (!isHuman[turn]) {
      String[] boardstr = new String[8];
      for (int i=0; i<8; i++) {
        String rowstr = "";
        for (int j=0; j<8; j++) {
          switch (cells[i][j].getState()) {
            case 0:
              rowstr += ".";
              break;
            case 1:
              rowstr += "o";
              break;
            case -1:
              rowstr += "x";
              break;
          }
        }
        boardstr[i] = rowstr;
      }
      // System.out.println("ai turn");
      iot[turn].write(leftTime[turn], turn, -1, boardstr);
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

  public void startGame(SettingPanel white, SettingPanel black, JTextArea _kifu) {
    player[0] = white;
    player[1] = black;
    kifu = _kifu;

    for (int i=0; i<2; i++) {
      player[i].setStones(2);
      isHuman[i] = player[i].isHuman();
      if (!isHuman[i]) {
        //exec
        try {
          String path = player[i].getPath();
          if (path.equals("")) {
            System.out.println("invalid path");
            destroy_all();
            return;
          }
          System.out.println("opening " + path);
          ProcessBuilder pb = new ProcessBuilder(path);

          process[i] = pb.start();
          iot[i] = new IOThread(
              process[i].getInputStream(),
              process[i].getOutputStream(),
              this,
              i
            );
          it[i] = new InputThread(
              process[i].getErrorStream(),
              this,
              i
            );
          iot[i].start();
          it[i].start();
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }
      }
      leftTime[i] = ((long)player[i].getTime())*1000000000;
      player[i].setLeftTime(leftTime[i]);
      player[i].clearIO();
    }

    turn = 0;
    running = true;
    waiting = true;
    startThinking = System.nanoTime();
    kifu.setText("--begin--\n");
    if (!isHuman[turn]) {
      String[] boardstr = {
        "........",
        "........",
        "........",
        "...ox...",
        "...xo...",
        "........",
        "........",
        "........"
      };
      iot[turn].write(leftTime[turn], turn, -1, boardstr);
    }

    loopTimer = new Timer(30, this);
    loopTimer.start();
  }

  public void actionPerformed(ActionEvent e) {
    if (!running) {
      loopTimer.stop();
      destroy_all();
      return;
    }
    if (waiting) {
      long passed = System.nanoTime() - startThinking;
      if (leftTime[turn] - passed <= 0) {
        player[turn].setLeftTime(0);
        // TLE
        kifu.append("##TLE##\n");
        halt();
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
  public void setLeftTime(long nanosec) {
    long millisec = nanosec/1000000;
    timer.setText( String.format("%d:%02d.%03d", millisec/1000/60, millisec/1000%60, millisec%1000) );
  }
  public int getTime() {
    return ti.getTime();
  }

  public void Cout(String line) {
    cout.append(line + "\n");
  }
  public void Cerr(String line) {
    cerr.append(line + "\n");
  }

  public void clearIO() {
    cout.setText("");
    cerr.setText("");
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
