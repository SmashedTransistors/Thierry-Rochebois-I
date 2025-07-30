import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class TRIEditor extends JPanel {

    //morpheme selection
    int a1, a0, b1, b0;
    int mFunc;
    int rev;
    int reso;
    //active pads
    int litPadA0, stateA0;
    int litPadA1 = 0, stateA1 = 0;
    int litPadB0, stateB0;
    int litPadB1, stateB1;
    int litPadLFF;
    DefaultListModel listModelMidiOut =new DefaultListModel<>();
    JList jlMidiOuts;

    JButton[] bPads = new JButton[40];
    JButton[] bRev = new JButton[8];
    JButton[] bReso = new JButton[6];
    JSlider sCutoff = new JSlider(JSlider.VERTICAL,0,127,127);
    JSlider sFMod = new JSlider(JSlider.VERTICAL,-64,63,0);
    JSlider sFEnv = new JSlider(JSlider.VERTICAL,0,127,127);

    JSlider sMrph = new JSlider(JSlider.VERTICAL,0,127,127);
    JSlider sMMod = new JSlider(JSlider.VERTICAL,0,127,0);
    JSlider sMRate = new JSlider(JSlider.VERTICAL,0,127,127);

    JSlider sVEnv = new JSlider(JSlider.VERTICAL,0,127,0);
    JSlider sVol = new JSlider(JSlider.VERTICAL,0,127,127);

    DefaultListModel listModelChorus =new DefaultListModel<>();
    JList jlChorus;

    DefaultListModel listModelSym =new DefaultListModel<>();
    JList jlSym;
    private void updatePadColors() {
        Color[] c = {Color.green, Color.orange, Color.red, Color.cyan, Color.white};
        deduce();
        for (int numPad = 0; numPad < 8; numPad++) {
            if (numPad != litPadA1) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateA1]);
            }
        }
        for (int numPad = 8; numPad < 16; numPad++) {
            int np = numPad - 8;
            if (np != litPadB1) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateB1]);
            }
        }
        Color funcOff=new Color(180,170,230);
        Color funcOn=new Color(220,90,255);
        for (int numPad = 16; numPad < 24; numPad++) {
            int np = numPad - 16;
            if (np != litPadLFF) {
                bPads[numPad].setBackground(funcOff);
            } else {
                bPads[numPad].setBackground(funcOn);
            }
        }
        for (int numPad = 24; numPad < 32; numPad++) {
            int np = numPad - 24;
            if (np != litPadA0) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateA0]);
            }
        }
        for (int numPad = 32; numPad < 40; numPad++) {
            int np = numPad - 32;
            if (np != litPadB0) {
                bPads[numPad].setBackground(Color.lightGray);
            } else {
                bPads[numPad].setBackground(c[stateB0]);
            }
        }
    }
    private void updateRevColors(){
        Color revOff=new Color(200,160,160);
        for (int numRev = 0; numRev < 8; numRev++) {
            if (numRev != rev) {
                bRev[numRev].setBackground(revOff);
            } else {
                bRev[numRev].setBackground(Color.red);
            }
        }
    }
    private void updateResoColors(){
        Color resoOff=new Color(200,230,200);
        for (int numReso = 0; numReso < 6; numReso++) {
            if (numReso != 5-reso) {
                bReso[numReso].setBackground(resoOff);
            } else {
                bReso[numReso].setBackground(Color.green);
            }
        }
    }

    ArrayList<String> midiDeviceNames;
    ArrayList<MidiDevice> midiDevices;

    void sendCC(int cc, int val){
        int sel=jlMidiOuts.getSelectedIndex();
        if(sel>=midiDevices.size() || sel<0){
            scanMidiDevices();
            return;
        }
        MidiDevice md=midiDevices.get(sel);
        try {
            md.open();
            Receiver receiver = md.getReceiver();
            ShortMessage m = new ShortMessage();
            m.setMessage(ShortMessage.CONTROL_CHANGE, 0, cc, val);
            receiver.send(m,-1);
            receiver.close();
            md.close();
        } catch (MidiUnavailableException e) {
            scanMidiDevices();
           // throw new RuntimeException(e);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    void scanMidiDevices(){
        midiDeviceNames=new ArrayList<>();
        midiDevices=new ArrayList<>();
        //MIDI
        MidiDevice.Info[] deviceInfo = MidiSystem.getMidiDeviceInfo();
        if (deviceInfo.length == 0) {
            System.out.println("No MIDI devices found");
            return;
        }

        for (MidiDevice.Info info : deviceInfo) {
            System.out.println("**********************");
            System.out.println("Device name: " + info.getName());
            System.out.println("Description: " + info.getDescription());
            System.out.println("Vendor: " + info.getVendor());
            System.out.println("Version: " + info.getVersion());

            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if(device.getMaxReceivers()!=0){
                    midiDeviceNames.add(info.getName());
                    midiDevices.add(device);
                }

                printDeviceType(device);
                System.out.println("Maximum receivers: "
                        + maxToString(device.getMaxReceivers()));
                System.out.println("Maximum transmitters: "
                        + maxToString(device.getMaxTransmitters()));
            } catch (MidiUnavailableException e) {

                System.out.println("Can't get MIDI device");
                e.printStackTrace();
            }
        }
        listModelMidiOut.clear();
        for(String n : midiDeviceNames){
            listModelMidiOut.addElement(n);
        }



    }
    void autoSelectTeensy(){
        for (int i = 0; i < listModelMidiOut.getSize(); i++) {
            if(((String) listModelMidiOut.get(i)).startsWith("Teensy")){
                jlMidiOuts.setSelectedIndex(i);
            }
        }
    }

    JPanel slPanel(JSlider sl, String label, Color labelColor)
    {
        JPanel panel = new JPanel();
        BoxLayout bl = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(bl);

        sl.setMajorTickSpacing(16);
        sl.setPaintTicks(true);
        sl.setPaintLabels(true);
        JLabel lbl=new JLabel(label, JLabel.LEFT);
        lbl.setForeground(labelColor);
        panel.add(lbl);
        panel.add(sl);
        return panel;
    }

    // Method
    TRIEditor() {

        scanMidiDevices();
        //Create the list and put it in a scroll pane.
        jlMidiOuts = new JList(listModelMidiOut);
        jlMidiOuts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlMidiOuts.setSelectedIndex(0);
        //jlMidiOuts.addListSelectionListener(this);
        jlMidiOuts.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(jlMidiOuts);


        this.add(jlMidiOuts);
        autoSelectTeensy();


        JPanel grPads = new JPanel();
        GridLayout glPads = new GridLayout(7, 10,5,5);
        grPads.setLayout(glPads);
        String colNames[]={"","SAW","STR","SYN","PLK","DXW","TOY","GRG","END","Reso"};
        for (int col = 0; col < 10; col++) {

            JLabel l = new JLabel(colNames[col],JLabel.CENTER);
            grPads.add(l);
        }
        String rowNames[]={"Morph A1","Morph B1","Func","Morph A0","Morph B0"};
        String funcNames[]={"Decay","LFO","Dec+LFO","Dec+fLFO","comLFO","LFOs","velDec","rand"};
        for (int row = 0; row < 5; row++) {

            JLabel l=new JLabel(rowNames[row]);
            grPads.add(l);
            for (int col = 0; col < 8; col++) {
                int numPad=row*8+col;
                if(row!=2) {
                    bPads[numPad] = new JButton();
                }else{
                    bPads[numPad] = new JButton(funcNames[col]);
                    bPads[numPad].setMargin(new Insets(0,0,0,0));
                }
                bPads[numPad].setPreferredSize(new Dimension(70, 35));
                bPads[numPad].setName(String.format("%d", numPad));
                // bPads[i].setBackground(c[i/8]);
                bPads[numPad].addActionListener(e -> {
                    padClicked(e);
                });
                grPads.add(bPads[numPad]);
            }
            //reso
            bReso[row] = new JButton();
            bReso[row].setPreferredSize(new Dimension(50, 25));
            bReso[row].setName(String.format("%d", row));
            // bPads[i].setBackground(c[i/8]);
            bReso[row].addActionListener(e -> {
                resoClicked(e);
            });
            grPads.add(bReso[row]);
        }
        //reverb buttons
        {
            JLabel l = new JLabel("Reverb");
            grPads.add(l);
            for (int col = 0; col < 8; col++) {

                bRev[col] = new JButton();
                bRev[col].setPreferredSize(new Dimension(20, 20));
                bRev[col].setMargin(new Insets(5, 5, 5, 5));
                bRev[col].setName(String.format("%d", col));
                // bPads[i].setBackground(c[i/8]);
                bRev[col].addActionListener(e -> {
                    revClicked(e);
                });
                grPads.add(bRev[col]);
            }
        }
        bReso[5] = new JButton("");
        bReso[5].setPreferredSize(new Dimension(50, 25));
        bReso[5].setName(String.format("%d", 5));
        // bPads[i].setBackground(c[i/8]);
        bReso[5].addActionListener(e -> {
            resoClicked(e);
        });
        grPads.add(bReso[5]);

        updatePadColors();
        updateRevColors();
        updateResoColors();
        this.add(grPads);


        sCutoff.addChangeListener(e -> {cutoffChanged(e);  });
        this.add(slPanel(sCutoff,"FCut", Color.red));
        sFMod.addChangeListener(e -> {fModChanged(e);      });
        this.add(slPanel(sFMod,"FMod", Color.red));
        sFEnv.addChangeListener(e -> {fEnvChanged(e);      });
        this.add(slPanel(sFEnv,"FEnv", Color.red));

        sMrph.addChangeListener(e -> {mrphChanged(e);  });
        this.add(slPanel(sMrph,"Mrph", Color.orange.darker()));
        sMMod.addChangeListener(e -> {mModChanged(e);      });
        this.add(slPanel(sMMod,"MMod", Color.orange.darker()));
        sMRate.addChangeListener(e -> {mRateChanged(e);      });
        this.add(slPanel(sMRate,"MRate", Color.orange.darker()));

        sVEnv.addChangeListener(e -> {vEnvChanged(e);      });
        this.add(slPanel(sVEnv,"VEnv", Color.black));
        sVol.addChangeListener(e -> {volChanged(e);      });
        this.add(slPanel(sVol,"Vol", Color.black));

        //Create the list and put it in a scroll pane.
        listModelChorus.addElement("Chorus 0");
        listModelChorus.addElement("Chorus 1");
        listModelChorus.addElement("Chorus 2");
        listModelChorus.addElement("Chorus 3");
        jlChorus = new JList(listModelChorus);
        jlChorus.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlChorus.setSelectedIndex(0);
        jlChorus.addListSelectionListener(e -> {chorusSelected(e);      });
        //jlMidiOuts.addListSelectionListener(this);
        jlChorus.setVisibleRowCount(4);

        listModelSym.addElement("Tri 1/2");
        listModelSym.addElement("Tri 1/3");
        listModelSym.addElement("Tri 1/4");
        listModelSym.addElement("Tri .49");
        listModelSym.addElement("Sin 1/2");
        listModelSym.addElement("Sin 1/3");
        listModelSym.addElement("Sin 1/4");
        listModelSym.addElement("Sin .49");
        jlSym = new JList(listModelSym);
        jlSym.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jlSym.setSelectedIndex(0);
        jlSym.addListSelectionListener(e -> {symSelected(e);      });
        //jlMidiOuts.addListSelectionListener(this);
        jlSym.setVisibleRowCount(8);


        this.add(jlChorus);
        this.add(jlSym);



        this.setVisible(true);
    }

    void deduce() {
        stateA1 = a1 % 4;
        litPadA1 = a1 / 4;
        stateA0 = a0 % 4;
        litPadA0 = a0 / 4;
        if (b1 == 32) {
            litPadB1 = litPadA1;
            stateB1 = 4;
        } else {
            stateB1 = b1 % 4;
            litPadB1 = b1 / 4;
        }
        if (b0 == 32) {
            litPadB0 = litPadA0;
            stateB0 = 4;
        } else {
            stateB0 = b0 % 4;
            litPadB0 = b0 / 4;
        }
    }

    void padClicked(ActionEvent e) {
        deduce();
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        int numPad = Integer.parseInt(b.getName());
        if (numPad < 8) {
            int n = numPad;
            if (n == litPadA1) {
                stateA1 = (stateA1 + 1) % 4;
            } else {
                litPadA1 = n;
            }
            a1 = litPadA1 * 4 + stateA1;
            sendCC(78,a1);
        } else if (numPad < 16) {
            int n = numPad - 8;
            if (n == litPadB1) {
                stateB1 = (stateB1 + 1) % 5;
            } else {
                litPadB1 = n;
            }
            if (stateB1 == 4)
                b1 = 32;
            else
                b1 = litPadB1 * 4 + stateB1;
            sendCC(79,b1);
        } else if (numPad < 24) {
            litPadLFF = numPad - 16;
            mFunc=litPadLFF;
            sendCC(77,mFunc);
        } else if (numPad < 32) {
            int n = numPad - 24;
            if (n == litPadA0) {
                stateA0 = (stateA0 + 1) % 4;
            } else {
                litPadA0 = n;
            }
            a0 = litPadA0 * 4 + stateA0;
            sendCC(75,a0);
        } else if (numPad < 40) {
            int n = numPad - 32;
            if (n == litPadB0) {
                stateB0 = (stateB0 + 1) % 5;
            } else {
                litPadB0 = n;
            }
            if (stateB0 == 4)
                b0 = 32;
            else
                b0 = litPadB0 * 4 + stateB0;
            sendCC(76,b0);
        }

        System.out.println("a1=" + a1);
        System.out.println("b1=" + b1);
        System.out.println("mFunc=" + mFunc);
        System.out.println("a0=" + a0);
        System.out.println("b0=" + b0);
        updatePadColors();

    }

    void revClicked(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        rev = Integer.parseInt(b.getName());
        sendCC(91,rev);
        updateRevColors();

    }
    void resoClicked(ActionEvent e) {
        JButton b = (JButton) e.getSource();
        System.out.println(b.getName());
        reso = 5-Integer.parseInt(b.getName());
        sendCC(71,reso);
        updateResoColors();

    }
    void cutoffChanged(ChangeEvent e) {
        System.out.println(sCutoff.getValue());
        sendCC(74,sCutoff.getValue());
    }
    void fModChanged(ChangeEvent e) {
        System.out.println(sFMod.getValue());
        sendCC(73,sFMod.getValue()+64);
    }
    void fEnvChanged(ChangeEvent e) {
        System.out.println(sFEnv.getValue());
        sendCC(72,sFEnv.getValue());
    }

    void mrphChanged(ChangeEvent e) {
        System.out.println(sMrph.getValue());
        sendCC(1,sMrph.getValue());
    }
    void mModChanged(ChangeEvent e) {
        System.out.println(sMMod.getValue());
        sendCC(18,sMMod.getValue());
    }
    void mRateChanged(ChangeEvent e) {
        System.out.println(sMRate.getValue());
        sendCC(19,sMRate.getValue());
    }

    void vEnvChanged(ChangeEvent e) {
        System.out.println(sVEnv.getValue());
        sendCC(16,sVEnv.getValue());
    }
    void volChanged(ChangeEvent e) {
        System.out.println(sVol.getValue());
        sendCC(7,sVol.getValue());
    }
    void chorusSelected(ListSelectionEvent e) {
        int sel=jlChorus.getSelectedIndex();
        sendCC(93,sel);
    }
    void symSelected(ListSelectionEvent e) {
        int sel=jlSym.getSelectedIndex();
        sendCC(94,sel);
    }
    private void printDeviceType(MidiDevice device) {
        if (device instanceof Sequencer) {
            System.out.println("This is a sequencer");
        } else if (device instanceof Synthesizer) {
            System.out.println("This is a synthesizer");
        } else {
            System.out.print("This is a MIDI port ");
            if (device.getMaxReceivers() != 0) {
                System.out.print("IN ");
            }
            if (device.getMaxTransmitters() != 0) {
                System.out.print("OUT ");
            }
            System.out.println();
        }
    }
    private String maxToString(int max) {
        return max == -1 ? "Unlimited" : String.valueOf(max);
    }
    // Main driver method
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("TRIEditor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new TRIEditor();
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

}
