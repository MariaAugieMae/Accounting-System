package ACCTG;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.math.BigDecimal;
import javax.swing.Timer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;


public class AccountingSystem extends JFrame {
	
	// Custom ComboBox UI at the NEW TRANSACTION debit/credit field
	class CustomComboBoxUI extends javax.swing.plaf.basic.BasicComboBoxUI {

	    private final Color borderColor = new Color(98, 98, 98);  // darker border
	    private final Color backgroundColor = Color.WHITE;
	    private final Color arrowBackground = new Color(230, 230, 230);
	    private final Color arrowColor = new Color(70, 70, 70);   // darker arrow
	    
	    @Override
	    protected JButton createArrowButton() {
	        JButton arrow = new JButton() {
			private static final long serialVersionUID = 1L;

				@Override
	            protected void paintComponent(Graphics g) {
	                super.paintComponent(g);
	                Graphics2D g2 = (Graphics2D) g;
	                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

	                int w = getWidth();
	                int h = getHeight();

	                g2.setColor(arrowBackground);
	                g2.fillRect(0, 0, w, h);

	                int size = 8;
	                int x = (w - size) / 2;
	                int y = (h - size) / 2 + 2;

	                g2.setColor(arrowColor);
	                int[] px = { x, x + size, x + size/2 };
	                int[] py = { y, y, y + size };
	                g2.fillPolygon(px, py, 3);
	            }
	        };

	        arrow.setBorder(BorderFactory.createLineBorder(borderColor));
	        arrow.setBackground(arrowBackground);
	        arrow.setFocusable(false);
	        return arrow;
	    }

	    @Override
	    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
	        g.setColor(backgroundColor);
	        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
	    }

	    @Override
	    protected void installDefaults() {
	        super.installDefaults();
	        comboBox.setBorder(BorderFactory.createLineBorder(borderColor, 1)); 
	        comboBox.setBackground(backgroundColor);
	        comboBox.setForeground(Color.DARK_GRAY);
	        comboBox.setFocusable(false);
	    }
	}
	
	class PlainComboRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
		private final Font plainFont = new Font("Segoe UI", Font.PLAIN, 13);

	    @Override
	    public Component getListCellRendererComponent(
	            JList<?> list, Object value, int index,
	            boolean isSelected, boolean cellHasFocus) {

	        JLabel label = (JLabel) super.getListCellRendererComponent(
	                list, value, index, isSelected, cellHasFocus);

	        label.setFont(plainFont);   // ALWAYS PLAIN — selected & dropdown
	        return label;
	    }
	}
	
	// Style Scrollbar inside ComboBox Popup 
	private void styleComboPopup(JComboBox<?> combo) {
	    combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
	        @Override
	        public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
	            try {
	                Object child = combo.getAccessibleContext().getAccessibleChild(0);
	                if (!(child instanceof javax.swing.JPopupMenu)) return;

	                javax.swing.JPopupMenu popup = (javax.swing.JPopupMenu) child;
	                javax.swing.JScrollPane sp = null;

	                // find the scroll pane inside popup
	                for (Component c : popup.getComponents()) {
	                    if (c instanceof javax.swing.JScrollPane) {
	                        sp = (javax.swing.JScrollPane) c;
	                        break;
	                    }
	                }
	                if (sp == null) return;

	                javax.swing.JScrollBar bar = sp.getVerticalScrollBar();
	                if (bar != null) {
	                    bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
	                        @Override protected void configureScrollBarColors() {
	                            thumbColor = new Color(0,150,150);
	                            trackColor = new Color(240,240,240);
	                        }
	                        @Override protected JButton createDecreaseButton(int orientation) {
	                            JButton b = super.createDecreaseButton(orientation);
	                            b.setBackground(trackColor);
	                            b.setBorder(BorderFactory.createEmptyBorder());
	                            return b;
	                        }
	                        @Override protected JButton createIncreaseButton(int orientation) {
	                            JButton b = super.createIncreaseButton(orientation);
	                            b.setBackground(trackColor);
	                            b.setBorder(BorderFactory.createEmptyBorder());
	                            return b;
	                        }
	                    });
	                }
	            } catch (Exception ex) {
	                System.out.println("ComboBox Scroll Styling Error: " + ex.getMessage());
	            }
	        }
	        @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
	        @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
	    });
	}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(AccountingSystem::new);
    }

    private static final long serialVersionUID = 1L;

    // Top-level UI fields
    private JPanel mainPanel;
    private JButton[] navButtons;
    private JPanel newTransactionPanel;
    private JPanel transactionsPanel;

    private final ArrayList<Transaction> transactions = new ArrayList<>();
    private DefaultTableModel transactionTableModel;

    public AccountingSystem() {
        setTitle("Accounting System");
        setSize(1500, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        String[] sections = { "New Transaction", "Transactions", "Accounts", "Balance Sheet", "General Journal", "General Ledger" };
        navButtons = new JButton[sections.length];

        JPanel navBar = new JPanel(new GridBagLayout());
        navBar.setBackground(new Color(240, 240, 240));
        navBar.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 180, 180)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        Color tabDefault = new Color(240, 240, 240);
        Color tabHover = new Color(250, 250, 250);
        Color tabActive = new Color(255, 255, 255);
        Color borderActive = new Color(0, 170, 170);
        Color shadow = new Color(200, 200, 200);

        for (int i = 0; i < sections.length; i++) {
            JButton btn = new JButton(sections[i]) {
                private static final long serialVersionUID = 1L;
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getClientProperty("active") == Boolean.TRUE) {
                        g2.setColor(shadow);
                        g2.fillRoundRect(2, getHeight() - 4, getWidth() - 4, 4, 8, 8);
                    }
                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 12, 12);
                    if (getClientProperty("active") == Boolean.TRUE) {
                        g2.setColor(borderActive);
                        g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            btn.setForeground(Color.BLACK);
            btn.setBackground(tabDefault);

            int index = i;
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (btn.getClientProperty("active") != Boolean.TRUE) {
                        btn.setBackground(tabHover);
                        btn.repaint();
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (btn.getClientProperty("active") != Boolean.TRUE) {
                        btn.setBackground(tabDefault);
                        btn.repaint();
                    }
                }
            });

            btn.addActionListener(e -> {
                for (JButton b : navButtons) {
                    if (b != null) {
                        b.putClientProperty("active", false);
                        b.setBackground(tabDefault);
                        b.repaint();
                    }
                }
                btn.putClientProperty("active", true);
                btn.setBackground(tabActive);
                btn.repaint();
                switchPanel(sections[index]);
            });

            navButtons[i] = btn;
            gbc.gridx = i;
            gbc.weightx = 0.0;
            navBar.add(btn, gbc);
        }

        gbc.gridx = sections.length;
        gbc.weightx = 1.0;
        navBar.add(Box.createHorizontalGlue(), gbc);

        navButtons[0].putClientProperty("active", true);
        navButtons[0].setBackground(tabActive);
        navButtons[0].repaint();

        add(navBar, BorderLayout.NORTH);

        // Create feature panels
        Accounts accountsPanel = new Accounts();
        BalanceSheet balanceSheetPanel = new BalanceSheet(accountsPanel);
        GeneralJournal generalJournalPanel = new GeneralJournal();
        GeneralLedger generalLedgerPanel = new GeneralLedger();

        // Link cross-panel updates
        accountsPanel.setBalanceSheetListener(balanceSheetPanel);
        accountsPanel.setGeneralJournalListener(generalJournalPanel);
        accountsPanel.setGeneralLedgerListener(generalLedgerPanel);

        newTransactionPanel = createNewTransactionPanel(accountsPanel, balanceSheetPanel, generalJournalPanel, generalLedgerPanel);
        transactionsPanel = createTransactionPanel();

        mainPanel = new JPanel(new CardLayout());
        mainPanel.add(newTransactionPanel, "New Transaction");
        mainPanel.add(transactionsPanel, "Transactions");
        mainPanel.add(accountsPanel, "Accounts");
        mainPanel.add(balanceSheetPanel, "Balance Sheet");
        mainPanel.add(generalJournalPanel, "General Journal");
        mainPanel.add(generalLedgerPanel, "General Ledger");

        add(mainPanel, BorderLayout.CENTER);
        switchPanel("New Transaction");
        setVisible(true);
    }

    private void switchPanel(String panelName) {
        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, panelName);
    }

    // Utility for Transactions table filtering
    public void refreshTransactionTable(String filter) {
        if (transactionTableModel == null) return;

        String f = (filter == null) ? "" : filter.trim().toLowerCase();
        transactionTableModel.setRowCount(0);

        for (Transaction t : transactions) {
            String date = safeString(t.getDate()).toLowerCase();
            String desc = safeString(t.getDescription()).toLowerCase();
            String debit = safeString(t.getDebitAccount()).toLowerCase();
            String credit = safeString(t.getCreditAccount()).toLowerCase();

            String amountStr;
            try {
                amountStr = BigDecimal.valueOf(t.getAmount()).toPlainString();
            } catch (Exception ex) {
                amountStr = String.valueOf(t.getAmount());
            }

            boolean matches = f.isEmpty()
                    || date.contains(f)
                    || desc.contains(f)
                    || debit.contains(f)
                    || credit.contains(f)
                    || amountStr.toLowerCase().contains(f);

            if (matches) {
                transactionTableModel.addRow(new Object[]{
                        t.getDate(), t.getDescription(), t.getDebitAccount(), t.getCreditAccount(), amountStr
                });
            }
        }

        int current = transactionTableModel.getRowCount();
        int toAdd = 30 - current;
        if (toAdd > 0) addBlankRows(transactionTableModel, toAdd);
    }

    private String safeString(Object o) { return o == null ? "" : o.toString(); }

    private void addBlankRows(DefaultTableModel model, int count) {
        for (int i = 0; i < count; i++) {
            model.addRow(new Object[]{"", "", "", "", ""});
        }
    }
    

    // New Transaction Panel
    private JPanel createNewTransactionPanel(Accounts accountsPanel,
            BalanceSheet balanceSheetPanel,
            GeneralJournal generalJournalPanel,
            GeneralLedger generalLedgerPanel) {

			JPanel panel = new JPanel(new BorderLayout());
			panel.setBackground(Color.WHITE);
			
			JPanel formPanel = new JPanel(null);
			formPanel.setPreferredSize(new Dimension(700, 350));
			formPanel.setBackground(Color.WHITE);
			
			JPanel topCenterWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 40));
			topCenterWrapper.setBackground(Color.WHITE);
			topCenterWrapper.add(formPanel);
			panel.add(topCenterWrapper, BorderLayout.NORTH);
			
			JLabel titleLabel = new JLabel("Add a New Transaction");
			titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
			titleLabel.setForeground(new Color(0, 150, 150));
			titleLabel.setBounds(107, 13, 250, 30);
			formPanel.add(titleLabel);
			
			
			// DATE LABEL
			ImageIcon calendarIcon = new ImageIcon(getClass().getResource("/icons/date.png"));
			Image scaledCalendarImage = calendarIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
			ImageIcon finalCalendarIcon = new ImageIcon(scaledCalendarImage);
			
			JLabel dateLabel = new JLabel(" Date :");
			dateLabel.setBounds(107, 60, 250, 30);  // same x & y
			dateLabel.setIcon(finalCalendarIcon);  // put icon on the left
			dateLabel.setIconTextGap(6);           // space between icon and text
			formPanel.add(dateLabel);
			
			JTextField dateField = new JTextField(" " + LocalDate.now().toString());
			dateField.setBounds(235, 60, 270, 30); 
			formPanel.add(dateField);
			
			
			// DESCRIPTION LABEL
			ImageIcon descIcon = new ImageIcon(getClass().getResource("/icons/description.png"));
			Image scaledDescImage = descIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
			ImageIcon finalDescIcon = new ImageIcon(scaledDescImage);
			
			JLabel descLabel = new JLabel("Description :");
			descLabel.setBounds(107, 100, 250, 30);
			descLabel.setIcon(finalDescIcon);
			descLabel.setIconTextGap(8);
			formPanel.add(descLabel);
			
			JTextField descField = new JTextField(" ");
			descField.setBounds(235, 100, 270, 30);
			formPanel.add(descField);
			
			
			// DEBIT LABEL
			ImageIcon debitIcon = new ImageIcon(getClass().getResource("/icons/debit.jpg"));
			Image scaledDebitImage = debitIcon.getImage().getScaledInstance(30, 25, Image.SCALE_SMOOTH);
			ImageIcon finalDebitIcon = new ImageIcon(scaledDebitImage);
			
			JLabel debitLabel = new JLabel("Debit Account :");
			debitLabel.setBounds(107, 140, 150, 30);
			debitLabel.setIcon(finalDebitIcon);
			debitLabel.setIconTextGap(5);
			formPanel.add(debitLabel);
			
			JComboBox<String> debitAccount = new JComboBox<>(new String[]{
			" Cash [ASSET]", " Accounts Receivable [ASSET]", " Inventory [ASSET]",
			" Prepaid Expenses [ASSET]", " Equipment [ASSET]", " Accumulated Depreciation [ASSET]",
			" Accounts Payable [LIABILITY]", " Notes Payable [LIABILITY]", " Owner's Capital [EQUITY]",
			" Service Revenue [INCOME]", " Sales Revenue [INCOME]", " Rent Expense [EXPENSE]",
			" Salaries Expense [EXPENSE]", " Supplies Expense [EXPENSE]", " Utilities Expense [EXPENSE]",
			" Cost of Good Sold [EXPENSE]"
			});
			debitAccount.setBounds(235, 140, 270, 30);
			debitAccount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			debitAccount.setRenderer(new PlainComboRenderer());
			formPanel.add(debitAccount);
			debitAccount.setUI(new CustomComboBoxUI());
			styleComboPopup(debitAccount);
			
			
			// CREDIT LABEL
			ImageIcon creditIcon = new ImageIcon(getClass().getResource("/icons/credit.jpg"));
			Image scaledCreditImage = creditIcon.getImage().getScaledInstance(30, 25, Image.SCALE_SMOOTH);
			ImageIcon finalCreditIcon = new ImageIcon(scaledCreditImage);
			
			JLabel creditLabel = new JLabel("Credit Account :");
			creditLabel.setBounds(107, 180, 150, 30);
			creditLabel.setIcon(finalCreditIcon);
			creditLabel.setIconTextGap(5);
			formPanel.add(creditLabel);
			
			JComboBox<String> creditAccount = new JComboBox<>(new String[]{
			" Cash [ASSET]", " Accounts Receivable [ASSET]", " Inventory [ASSET]",
			" Prepaid Expenses [ASSET]", " Equipment [ASSET]", " Accumulated Depreciation [ASSET]",
			" Accounts Payable [LIABILITY]", " Notes Payable [LIABILITY]", " Owner's Capital [EQUITY]",
			" Service Revenue [INCOME]", " Sales Revenue [INCOME]", " Rent Expense [EXPENSE]",
			" Salaries Expense [EXPENSE]", " Supplies Expense [EXPENSE]", " Utilities Expense [EXPENSE]",
			" Cost of Good Sold [EXPENSE]"
			});
			creditAccount.setBounds(235, 180, 270, 30);
			creditAccount.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			creditAccount.setRenderer(new PlainComboRenderer());
			formPanel.add(creditAccount);
			creditAccount.setUI(new CustomComboBoxUI());
			styleComboPopup(creditAccount);
			
			
			// AMOUNT LABEL
			ImageIcon amountIcon = new ImageIcon(getClass().getResource("/icons/amount.png"));
			Image scaledAmountImage = amountIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
			ImageIcon finalAmountIcon = new ImageIcon(scaledAmountImage);
			
			JLabel amountLabel = new JLabel("Amount :");
			amountLabel.setBounds(107, 220, 150, 30);
			amountLabel.setIcon(finalAmountIcon);
			amountLabel.setIconTextGap(8);
			formPanel.add(amountLabel);
			
			JTextField amountField = new JTextField(" ");
			amountField.setBounds(235, 220, 270, 30);
			formPanel.add(amountField);
			
			RoundedButton addBtn = new RoundedButton("Add Transaction");
			addBtn.setBounds(235, 260, 140, 30); //( x, y, width, height)
			formPanel.add(addBtn);
			
			RoundedButton clearBtn = new RoundedButton("Clear");
			clearBtn.setBounds(390, 260, 112, 30);
			formPanel.add(clearBtn);
			
			JLabel statusLabel = new JLabel(" ");
			statusLabel.setBounds(235, 295, 300, 25);
			formPanel.add(statusLabel);
			
			
			// Add Button Logic with validation
			addBtn.addActionListener(e -> {
			String date = dateField.getText().trim();
			String desc = descField.getText().trim();
			String debit = debitAccount.getSelectedItem().toString();
			String credit = creditAccount.getSelectedItem().toString();
			double amount;
			
			// Validate date
			try {
			LocalDate.parse(date);
			} catch (DateTimeParseException ex) {
			showStatus(statusLabel, "❌ Invalid date format (YYYY-MM-DD)", Color.RED);
			return;
			}
			
			// Validate amount
			try {
			amount = Double.parseDouble(amountField.getText().trim());
			if (amount <= 0) {
			showStatus(statusLabel, "❌ Amount must be greater than 0", Color.RED);
			return;
			}
			} catch (NumberFormatException ex) {
			showStatus(statusLabel, "❌  Invalid input. Please check all fields", Color.RED);
			return;
			}


            // Validate accounts
            if (debit.equals(credit)) {
                showStatus(statusLabel, "❌ Debit and Credit accounts cannot be the same", Color.RED);
                return;
            }

            Transaction t = new Transaction(date, desc, debit, credit, amount);
            transactions.add(t);
            refreshTransactionTable("");

            // Update linked panels
            if (accountsPanel != null) accountsPanel.updateFromTransaction(debit, credit, amount, desc, date);
            if (balanceSheetPanel != null) balanceSheetPanel.refreshData();

            showStatus(statusLabel, "✅ Transaction Saved Successfully", new Color(0, 170, 170));
            descField.setText("");
            amountField.setText("");
        });

        getRootPane().setDefaultButton(addBtn);

        // Clear button logic
        clearBtn.addActionListener(e -> {
            dateField.setText(LocalDate.now().toString());
            descField.setText("");
            amountField.setText("");
            debitAccount.setSelectedIndex(0);
            creditAccount.setSelectedIndex(0);
            statusLabel.setText(" ");
        });

        return panel;
    }

    private void showStatus(JLabel statusLabel, String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
        new Timer(3000, e -> statusLabel.setText(" ")).start();
    }

    // Transactions Panel
    private JPanel createTransactionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 150, 150)));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("Transactions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(0, 150, 150));
        title.setBorder(BorderFactory.createEmptyBorder(0, 13, 0, 0));

        JLabel subtitle = new JLabel("Complete record of business transactions");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 13, 4, 0));

        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(subtitle, BorderLayout.SOUTH);
        topPanel.add(titlePanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel("Search:");
        JTextField searchField = new JTextField(25);
        RoundedButton clearButton = new RoundedButton("Clear");

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(clearButton);

        topPanel.add(searchPanel, BorderLayout.EAST);

        panel.add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"Date", "Description", "Debit Account", "Credit Account", "Amount"};
        transactionTableModel = new DefaultTableModel(columnNames, 0) {
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = new JTable(transactionTableModel);
        table.setRowHeight(25);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setBackground(Color.WHITE);
        table.setForeground(Color.BLACK);
        table.setFillsViewportHeight(true);
        table.setShowGrid(true);
        table.setGridColor(new Color(0, 140, 140));
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0, 140, 140)));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                cell.setHorizontalAlignment(SwingConstants.LEFT);
                cell.setOpaque(true);
                cell.setBackground(isSelected ? new Color(220, 250, 250) : Color.WHITE);
                cell.setBorder(BorderFactory.createCompoundBorder(
                        cell.getBorder(),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
                return cell;
            }
        });

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(0, 170, 170));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setBackground(Color.WHITE);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);

        table.setPreferredScrollableViewportSize(new Dimension(1000, 420));

        // Vertical scrollbar
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            private final Color THUMB_COLOR = new Color(0, 150, 150);
            private final Color TRACK_COLOR = new Color(240, 240, 240);

            @Override protected void configureScrollBarColors() {
                this.thumbColor = THUMB_COLOR;
                this.trackColor = TRACK_COLOR;
            }
            @Override protected JButton createDecreaseButton(int orientation) {
                JButton button = super.createDecreaseButton(orientation);
                button.setBackground(TRACK_COLOR);
                button.setBorder(BorderFactory.createEmptyBorder());
                return button;
            }
            @Override protected JButton createIncreaseButton(int orientation) {
                JButton button = super.createIncreaseButton(orientation);
                button.setBackground(TRACK_COLOR);
                button.setBorder(BorderFactory.createEmptyBorder());
                return button;
            }
        });

        // Horizontal scrollbar
        JScrollBar horizontalBar = scrollPane.getHorizontalScrollBar();
        horizontalBar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            private final Color THUMB_COLOR = new Color(0, 150, 150);
            private final Color TRACK_COLOR = new Color(240, 240, 240);

            @Override protected void configureScrollBarColors() {
                this.thumbColor = THUMB_COLOR;
                this.trackColor = TRACK_COLOR;
            }
            @Override protected JButton createDecreaseButton(int orientation) {
                JButton button = super.createDecreaseButton(orientation);
                button.setBackground(TRACK_COLOR);
                button.setBorder(BorderFactory.createEmptyBorder());
                return button;
            }
            @Override protected JButton createIncreaseButton(int orientation) {
                JButton button = super.createIncreaseButton(orientation);
                button.setBackground(TRACK_COLOR);
                button.setBorder(BorderFactory.createEmptyBorder());
                return button;
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);

        // Initial fill
        addBlankRows(transactionTableModel, 30);

        // Search logic
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                refreshTransactionTable(searchField.getText().toLowerCase());
            }
        });

        clearButton.addActionListener(e -> {
            searchField.setText("");
            refreshTransactionTable("");
        });

        return panel;
    }

    // -------------------- Nested Classes --------------------

    // RoundedButton
    static class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;
        private boolean hover;

        public RoundedButton(String text) {
            this(text, null);
        }

        public RoundedButton(String text, Icon icon) {
            super(text, icon);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setHorizontalTextPosition(SwingConstants.RIGHT);
            setIconTextGap(5);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color base1 = new Color(0, 170, 170);
            Color base2 = new Color(0, 140, 140);
            Color hover1 = new Color(0, 190, 190);
            Color hover2 = new Color(0, 160, 160);

            GradientPaint gradient = new GradientPaint(
                    0, 0, hover ? hover1 : base1,
                    0, getHeight(), hover ? hover2 : base2
            );

            int arc = 12;
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            g2.setColor(new Color(0, 0, 0, 50));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

            super.paintComponent(g2);
            g2.dispose();
        }
    }

    // Transaction
    static class Transaction {
        private final String date, description, debitAccount, creditAccount;
        private final double amount;
        public Transaction(String date, String description, String debitAccount, String creditAccount, double amount) {
            this.date = date; this.description = description; this.debitAccount = debitAccount; this.creditAccount = creditAccount; this.amount = amount;
        }
        public String getDate() { return date; }
        public String getDescription() { return description; }
        public String getDebitAccount() { return debitAccount; }
        public String getCreditAccount() { return creditAccount; }
        public double getAmount() { return amount; }
    }

    // Accounts
    static class Accounts extends JPanel {
        private static final long serialVersionUID = 1L;

        private JTable table;
        private DefaultTableModel model;
        private final ArrayList<Account> accountList;

        // Linked panels
        private BalanceSheet balanceSheet;
        private GeneralJournal generalJournal;
        private GeneralLedger generalLedger;

        // Colors
        private final Color COLOR_ASSET = new Color(0, 200, 255, 40);
        private final Color COLOR_LIABILITY = new Color(255, 140, 0, 40);
        private final Color COLOR_EQUITY = new Color(153, 102, 255, 40);
        private final Color COLOR_INCOME = new Color(0, 200, 100, 40);
        private final Color COLOR_EXPENSE = new Color(255, 50, 50, 40);
        private final Color LINE_COLOR = new Color(0, 150, 150);

        public Accounts() {
            setLayout(new BorderLayout(0, 0));
            setBackground(Color.WHITE);

            accountList = new ArrayList<>();

            // Header
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(Color.WHITE);
            topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, LINE_COLOR));

            JLabel title = new JLabel("Accounts");
            title.setFont(new Font("Segoe UI", Font.BOLD, 20));
            title.setForeground(LINE_COLOR);
            title.setBorder(BorderFactory.createEmptyBorder(3, 13, 3, 0));
            topPanel.add(title, BorderLayout.WEST);

            JLabel subtitle = new JLabel("List of all ledger accounts and current balances");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            subtitle.setBorder(BorderFactory.createEmptyBorder(0, 13, 4, 0));
            topPanel.add(subtitle, BorderLayout.SOUTH);

            add(topPanel, BorderLayout.NORTH);

            String[] columns = {"Account", "Type", "Balance"};
            model = new DefaultTableModel(columns, 0) {
                private static final long serialVersionUID = 1L;
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };

            table = new JTable(model);
            table.setShowGrid(false);
            table.setIntercellSpacing(new Dimension(0, 0));
            table.setBackground(Color.WHITE);
            table.setForeground(Color.BLACK);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            table.setRowHeight(25);
            table.setFillsViewportHeight(true);

            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;
                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel cell = (JLabel) super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);
                    cell.setHorizontalAlignment(SwingConstants.LEFT);
                    cell.setOpaque(true);

                    String type = table.getValueAt(row, 1).toString();
                    Color bg = Color.WHITE;
                    if (column == 0) {
                        switch (type.toUpperCase()) {
                            case "ASSET": bg = COLOR_ASSET; break;
                            case "LIABILITY": bg = COLOR_LIABILITY; break;
                            case "EQUITY": bg = COLOR_EQUITY; break;
                            case "INCOME": bg = COLOR_INCOME; break;
                            case "EXPENSE": bg = COLOR_EXPENSE; break;
                        }
                    }

                    cell.setBorder(BorderFactory.createMatteBorder(0, (column == 0) ? 1 : 0, 1, 1, LINE_COLOR));
                    cell.setBackground(isSelected ? new Color(220, 250, 250) : bg);

                    if (column == 2 && value instanceof Double) {
                        cell.setText(String.format("%.2f", Math.abs((Double) value)));
                    }

                    cell.setBorder(BorderFactory.createCompoundBorder(
                            cell.getBorder(),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)
                    ));

                    return cell;
                }
            });

            JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Segoe UI", Font.BOLD, 14));
            header.setBackground(new Color(0, 170, 170));
            header.setForeground(Color.WHITE);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getViewport().setBackground(Color.WHITE);

            JPanel marginPanel = new JPanel(new BorderLayout());
            marginPanel.setBackground(Color.WHITE);
            marginPanel.setBorder(BorderFactory.createEmptyBorder(9, 16, 16, 16));
            marginPanel.add(scrollPane, BorderLayout.CENTER);
            add(marginPanel, BorderLayout.CENTER);

            addAllAccounts();
            refreshTable();
        }

        public void updateFromTransaction(String debit, String credit, double amount, String description, String date) {
            String cleanDebit = debit.split("\\[")[0].trim();
            String cleanCredit = credit.split("\\[")[0].trim();

            for (Account acc : accountList) {
                if (acc.getName().equalsIgnoreCase(cleanDebit)) acc.debit(amount);
                if (acc.getName().equalsIgnoreCase(cleanCredit)) acc.credit(amount);
            }
            refreshTable();

            if (balanceSheet != null) balanceSheet.refreshData();

            if (generalJournal != null) {
                Transaction t = new Transaction(date, description, cleanDebit, cleanCredit, amount);
                generalJournal.addTransactionEntry(t);
            }

            if (generalLedger != null) {
                Transaction t = new Transaction(date, description, cleanDebit, cleanCredit, amount);
                generalLedger.addTransactionEntry(t);
            }
        }

        public ArrayList<Object[]> getAccountBalances() {
            ArrayList<Object[]> balances = new ArrayList<>();
            for (Account acc : accountList) {
                balances.add(new Object[]{acc.getName(), acc.getType(), acc.getBalance()});
            }
            return balances;
        }

        public void setBalanceSheetListener(BalanceSheet sheet) { this.balanceSheet = sheet; }
        public void setGeneralJournalListener(GeneralJournal journal) { this.generalJournal = journal; }
        public void setGeneralLedgerListener(GeneralLedger ledger) { this.generalLedger = ledger; }

        private void addAllAccounts() {
            String[][] accounts = {
                    {"Cash", "ASSET"},
                    {"Accounts Receivable", "ASSET"},
                    {"Inventory", "ASSET"},
                    {"Prepaid Expenses", "ASSET"},
                    {"Equipment", "ASSET"},
                    {"Accumulated Depreciation", "ASSET"},
                    {"Accounts Payable", "LIABILITY"},
                    {"Notes Payable", "LIABILITY"},
                    {"Owner's Capital", "EQUITY"},
                    {"Service Revenue", "INCOME"},
                    {"Sales Revenue", "INCOME"},
                    {"Rent Expense", "EXPENSE"},
                    {"Salaries Expense", "EXPENSE"},
                    {"Supplies Expense", "EXPENSE"},
                    {"Utilities Expense", "EXPENSE"},
                    {"Cost of Goods Sold", "EXPENSE"}
            };
            for (String[] acc : accounts) accountList.add(new Account(acc[0], acc[1], 0.0));
        }

        private void refreshTable() {
            model.setRowCount(0);
            for (Account acc : accountList)
                model.addRow(new Object[]{acc.getName(), acc.getType(), acc.getBalance()});
        }

        private static class Account {
            private final String name;
            private final String type;
            private double balance;

            public Account(String name, String type, double balance) { this.name = name; this.type = type; this.balance = balance; }
            public String getName() { return name; }
            public String getType() { return type; }
            public Double getBalance() { return balance; }

            public void debit(double amount) {
                if (type.equals("ASSET") || type.equals("EXPENSE")) balance += amount;
                else balance -= amount;
            }

            public void credit(double amount) {
                if (type.equals("ASSET") || type.equals("EXPENSE")) balance -= amount;
                else balance += amount;
            }
        }
    }

    // General Journal
    static class GeneralJournal extends JPanel {
        private static final long serialVersionUID = 1L;
        private JTable journalTable;
        private DefaultTableModel journalModel;

        private final Color LINE_COLOR = new Color(0, 150, 150);
        private final Color HEADER_COLOR = new Color(0, 170, 170);

        private final ArrayList<Transaction> transactions = new ArrayList<>();

        public GeneralJournal() {
            setLayout(new BorderLayout(0, 0));
            setBackground(Color.WHITE);

            // Top panel
            JPanel topPanel = new JPanel(new GridBagLayout());
            topPanel.setBackground(Color.WHITE);
            topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, LINE_COLOR));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 10, 5, 10);

            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.setBackground(Color.WHITE);

            JLabel title = new JLabel("General Journal");
            title.setFont(new Font("Segoe UI", Font.BOLD, 20));
            title.setForeground(LINE_COLOR);

            JLabel subtitle = new JLabel("Chronological record of all journal entries");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            titlePanel.add(title, BorderLayout.NORTH);
            titlePanel.add(subtitle, BorderLayout.SOUTH);

            // Print button (icon optional)
            ImageIcon printIcon = null;
            try {
                printIcon = new ImageIcon(getClass().getResource("/icons/printer.png"));
            } catch (Exception ignored) {}
            Image scaledPrintImage = printIcon != null ? printIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH) : null;
            RoundedButton printButton = new RoundedButton(" Print", scaledPrintImage != null ? new ImageIcon(scaledPrintImage) : null);
            printButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            printButton.setForeground(Color.WHITE);
            printButton.setBackground(HEADER_COLOR);
            printButton.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            printButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            printButton.setFocusable(false);

            // Layout
            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0;
            topPanel.add(titlePanel, gbc);
            gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0;
            topPanel.add(printButton, gbc);

            add(topPanel, BorderLayout.NORTH);

            // Table
            String[] columns = {"Date", "Description", "Account", "Debit", "Credit"};
            journalModel = new DefaultTableModel(columns, 0) {
                private static final long serialVersionUID = 1L;
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };

            journalTable = createStyledTable(journalModel);
            addBlankRows(journalModel, 30);

            JScrollPane scrollPane = new JScrollPane(journalTable);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setBackground(Color.WHITE);
            customizeScrollBar(scrollPane.getVerticalScrollBar());
            customizeScrollBar(scrollPane.getHorizontalScrollBar());
            add(scrollPane, BorderLayout.CENTER);
        }

        public void addTransactionEntry(Transaction t) {
            transactions.add(t);
            refreshJournal();
        }

        private void refreshJournal() {
            journalModel.setRowCount(0);
            for (Transaction t : transactions) {
                journalModel.addRow(new Object[]{ t.getDate(), t.getDescription(), t.getDebitAccount(), String.format("%.2f", t.getAmount()), "" });
                journalModel.addRow(new Object[]{ t.getDate(), t.getDescription(), t.getCreditAccount(), "", String.format("%.2f", t.getAmount()) });
            }
            addBlankRows(journalModel, 30);
        }

        private void addBlankRows(DefaultTableModel model, int count) {
            for (int i = 0; i < count; i++) model.addRow(new Object[]{"", "", "", "", ""});
        }

        private JTable createStyledTable(DefaultTableModel model) {
            JTable table = new JTable(model);
            table.setShowGrid(true);
            table.setGridColor(new Color(0, 140, 140));
            table.setIntercellSpacing(new Dimension(1, 1));
            table.setBackground(Color.WHITE);
            table.setForeground(Color.BLACK);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            table.setRowHeight(25);
            table.setFillsViewportHeight(true);
            table.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, LINE_COLOR));


            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                    JLabel cell = (JLabel) super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, col);
                    cell.setOpaque(true);
                    cell.setBackground(isSelected ? new Color(220, 250, 250) : Color.WHITE);
                    cell.setBorder(BorderFactory.createCompoundBorder(
                            cell.getBorder(),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)
                    ));
                    return cell;
                }
            });

            JTableHeader header = table.getTableHeader();
            header.setBackground(HEADER_COLOR);
            header.setForeground(Color.WHITE);
            header.setFont(new Font("Segoe UI", Font.BOLD, 14));
            header.setReorderingAllowed(false);
            return table;
        }

        private void customizeScrollBar(JScrollBar bar) {
            if (bar == null) return;
            bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                private final Color THUMB_COLOR = new Color(0, 150, 150);
                private final Color TRACK_COLOR = new Color(240, 240, 240);

                @Override protected void configureScrollBarColors() {
                    this.thumbColor = THUMB_COLOR;
                    this.trackColor = TRACK_COLOR;
                }

                @Override protected JButton createDecreaseButton(int orientation) {
                    JButton button = super.createDecreaseButton(orientation);
                    button.setBackground(TRACK_COLOR);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    return button;
                }

                @Override protected JButton createIncreaseButton(int orientation) {
                    JButton button = super.createIncreaseButton(orientation);
                    button.setBackground(TRACK_COLOR);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    return button;
                }
            });
        }
    }

 // Balance Sheet
    static class BalanceSheet extends JPanel {
        private static final long serialVersionUID = 1L;

        private JTable assetsTable;
        private JTable liabilitiesTable;
        private DefaultTableModel assetsModel;
        private DefaultTableModel liabilitiesModel;
        private final Accounts accountsDataSource;

        private final Color LINE_COLOR = new Color(0, 150, 150);
        private final Color HEADER_COLOR = new Color(0, 170, 170);
        private final Color TOTAL_HIGHLIGHT = new Color(220, 250, 250);
        private final Color CELL_FG = Color.BLACK;

        public BalanceSheet(Accounts accountsDataSource) {
            this.accountsDataSource = accountsDataSource;
            setLayout(new BorderLayout(0, 0));
            setBackground(Color.WHITE);

            JPanel topPanel = new JPanel(new GridBagLayout());
            topPanel.setBackground(Color.WHITE);
            topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, LINE_COLOR));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 10, 5, 10);

            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.setBackground(Color.WHITE);

            JLabel title = new JLabel("Balance Sheet");
            title.setFont(new Font("Segoe UI", Font.BOLD, 20));
            title.setForeground(LINE_COLOR);

            JLabel subtitle = new JLabel("Statement of assets, liabilities, and equity");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            titlePanel.add(title, BorderLayout.NORTH);
            titlePanel.add(subtitle, BorderLayout.SOUTH);

            ImageIcon printIcon = null;
            try {
                printIcon = new ImageIcon(getClass().getResource("/icons/printer.png"));
            } catch (Exception ignored) {}
            Image scaledPrintImage = printIcon != null ? printIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH) : null;
            RoundedButton printButton = new RoundedButton(" Print", scaledPrintImage != null ? new ImageIcon(scaledPrintImage) : null);
            printButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
            printButton.setForeground(Color.WHITE);
            printButton.setBackground(HEADER_COLOR);
            printButton.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
            printButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            printButton.setFocusable(false);

            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0;
            topPanel.add(titlePanel, gbc);
            gbc.gridx = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0;
            topPanel.add(printButton, gbc);
            add(topPanel, BorderLayout.NORTH);

            // Content layout
            JPanel contentPanel = new JPanel(new GridLayout(1, 2, 16, 0));
            contentPanel.setBackground(Color.WHITE);
            contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

            // Left - Assets
            JPanel leftPanel = new JPanel(new BorderLayout());
            leftPanel.setBackground(Color.WHITE);

            JLabel assetLabel = new JLabel("Assets");
            assetLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            assetLabel.setForeground(LINE_COLOR);
            assetLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 0));
            leftPanel.add(assetLabel, BorderLayout.NORTH);

            String[] assetCols = {"Account", "Amount"};
            assetsModel = new DefaultTableModel(assetCols, 0);
            assetsTable = createStyledTable(assetsModel);
            JScrollPane assetScroll = new JScrollPane(assetsTable);
            assetScroll.setBorder(BorderFactory.createEmptyBorder());
            assetScroll.getViewport().setBackground(Color.WHITE);
            leftPanel.add(assetScroll, BorderLayout.CENTER);

            // Right - Liabilities & Equity
            JPanel rightPanel = new JPanel(new BorderLayout());
            rightPanel.setBackground(Color.WHITE);

            JLabel liabLabel = new JLabel("Liabilities & Equity");
            liabLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            liabLabel.setForeground(LINE_COLOR);
            liabLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 0));
            rightPanel.add(liabLabel, BorderLayout.NORTH);

            String[] liabCols = {"Account", "Amount"};
            liabilitiesModel = new DefaultTableModel(liabCols, 0);
            liabilitiesTable = createStyledTable(liabilitiesModel);
            JScrollPane liabScroll = new JScrollPane(liabilitiesTable);
            liabScroll.setBorder(BorderFactory.createEmptyBorder());
            liabScroll.getViewport().setBackground(Color.WHITE);
            rightPanel.add(liabScroll, BorderLayout.CENTER);

            contentPanel.add(leftPanel);
            contentPanel.add(rightPanel);

            add(contentPanel, BorderLayout.CENTER);

            refreshData();
        }

        public void refreshData() {
            if (accountsDataSource == null) return;

            assetsModel.setRowCount(0);
            liabilitiesModel.setRowCount(0);

            double totalAssets = 0.0;
            double totalLiabilities = 0.0;
            double totalEquity = 0.0;
            double totalIncome = 0.0;
            double totalExpense = 0.0;

            // Cell styling
            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;

                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column) {

                    JLabel cell = (JLabel) super.getTableCellRendererComponent(
                            table, value, isSelected, hasFocus, row, column);

                    Object firstColValue = table.getValueAt(row, 0);
                    boolean isTotalRow = firstColValue != null &&
                            firstColValue.toString().toLowerCase().contains("total");

                    if (isTotalRow) {
                        cell.setBackground(TOTAL_HIGHLIGHT);
                        cell.setForeground(Color.BLACK);
                        cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                    } else {
                        cell.setBackground(Color.WHITE);
                        cell.setForeground(CELL_FG);
                        cell.setFont(cell.getFont().deriveFont(Font.PLAIN));
                    }

                    cell.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 1, 1, LINE_COLOR),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)
                    ));

                    return cell;
                }
            };

            assetsTable.setDefaultRenderer(Object.class, cellRenderer);
            liabilitiesTable.setDefaultRenderer(Object.class, cellRenderer);

            // Process every account
            for (Object[] row : accountsDataSource.getAccountBalances()) {
                String account = (String) row[0];
                double balance = Double.parseDouble(row[2].toString());

                if (isContraAsset(account)) {
                	assetsModel.addRow(new Object[]{account, String.format("%.2f", Math.abs(balance))});
                	totalAssets -= Math.abs(balance);   // still subtract internally
                }
                else if (isAsset(account)) {
                	assetsModel.addRow(new Object[]{account, String.format("%.2f", Math.abs(balance))});
                    totalAssets += balance;
                }
                else if (isLiability(account)) {
                	liabilitiesModel.addRow(new Object[]{account, String.format("%.2f", Math.abs(balance))});
                    totalLiabilities += balance;
                }
                else if (isEquity(account)) {
                	liabilitiesModel.addRow(new Object[]{account, String.format("%.2f", Math.abs(balance))});
                    totalEquity += balance;
                }
                else if (isIncome(account)) {
                    totalIncome += balance;
                }
                else if (isExpense(account)) {
                    totalExpense += balance;
                }
            }

            // Net income → equity
            double netIncome = totalIncome - totalExpense;
            if (netIncome != 0) {
            	liabilitiesModel.addRow(new Object[]{"Net Income", String.format("%.2f", Math.abs(netIncome))});
                totalEquity += netIncome;
            }

            double totalLiabEquity = totalLiabilities + totalEquity;

            // Totals
            assetsModel.addRow(new Object[]{"", ""});
            assetsModel.addRow(new Object[]{"Total Assets", String.format("%.2f", totalAssets)});

            liabilitiesModel.addRow(new Object[]{"", ""});
            liabilitiesModel.addRow(new Object[]{"Total Liabilities + Equity", String.format("%.2f", totalLiabEquity)});
        }

        private boolean isAsset(String name) {
            name = name.toLowerCase();
            return name.contains("cash")
                    || name.contains("receivable")
                    || name.contains("inventory")
                    || name.contains("equipment")
                    || name.contains("prepaid")
                    || name.contains("supplies");
        }

        private boolean isContraAsset(String name) {
            return name.toLowerCase().contains("accumulated");
        }

        private boolean isLiability(String name) {
            name = name.toLowerCase();
            return name.contains("payable")
                    || name.contains("notes")
                    || name.contains("loan");
        }

        private boolean isEquity(String name) {
            name = name.toLowerCase();
            return name.contains("capital")
                    || name.contains("equity");
        }

        private boolean isIncome(String name) {
            name = name.toLowerCase();
            return name.contains("revenue")
                    || name.contains("income")
                    || name.contains("service");
        }

        private boolean isExpense(String name) {
            name = name.toLowerCase();
            return name.contains("expense")
                    || name.contains("cost of goods sold")
                    || name.contains("cogs");
        }

        private JTable createStyledTable(DefaultTableModel model) {
            JTable table = new JTable(model) {
			private static final long serialVersionUID = 1L;

				@Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    // Calculate bottom of the LAST REAL row
                    int lastRow = getRowCount() - 1;
                    if (lastRow >= 0) {
                        Rectangle rect = getCellRect(lastRow, 0, true);
                        int bottomY = rect.y + rect.height;

                        g.setColor(LINE_COLOR);

                        // Draw left border only up to the last real row
                        g.drawLine(0, 0, 0, bottomY);
                    }
                }
            };

            table.setShowGrid(true);
            table.setIntercellSpacing(new Dimension(0, 0));
            table.setBackground(Color.WHITE);
            table.setForeground(CELL_FG);
            table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            table.setRowHeight(26);
            table.setFillsViewportHeight(true);
            table.setBorder(BorderFactory.createEmptyBorder());

            JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Segoe UI", Font.BOLD, 14));
            header.setBackground(HEADER_COLOR);
            header.setForeground(Color.WHITE);
            header.setOpaque(true);

            return table;
        }

    }

    // General Ledger
    static class GeneralLedger extends JPanel {
        private static final long serialVersionUID = 1L;

        private DefaultTableModel ledgerTableModel;
        private JTable ledgerTable;
        private JComboBox<String> accountDropdown;
        private final Map<String, java.util.List<LedgerEntry>> ledgerData = new HashMap<>();

        public GeneralLedger() {
            setLayout(new BorderLayout(0, 0));
            setBackground(Color.WHITE);

            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(Color.WHITE);
            topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 150, 150)));

            JPanel titlePanel = new JPanel(new BorderLayout());
            titlePanel.setBackground(Color.WHITE);
            titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 13, 5, 0));

            JLabel title = new JLabel("General Ledger");
            title.setFont(new Font("Segoe UI", Font.BOLD, 20));
            title.setForeground(new Color(0, 150, 150));

            JLabel subtitle = new JLabel("Detailed posting of transactions per account");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            titlePanel.add(title, BorderLayout.NORTH);
            titlePanel.add(subtitle, BorderLayout.SOUTH);
            topPanel.add(titlePanel, BorderLayout.WEST);

            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            rightPanel.setOpaque(false);
            rightPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 16));

            JLabel accountLabel = new JLabel("Account:");
            accountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));

            accountDropdown = new JComboBox<>();
            accountDropdown.setMaximumRowCount(8); // ensures scrollpane exists
            customizeComboPopupScrollBar(accountDropdown);
            accountDropdown.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            accountDropdown.setPreferredSize(new Dimension(220, 26));
            accountDropdown.setBackground(Color.WHITE);
            accountDropdown.setForeground(Color.BLACK);
            accountDropdown.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

            accountDropdown.setRenderer(new DefaultListCellRenderer() {
                private static final long serialVersionUID = 1L;
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    label.setOpaque(true);
                    label.setBackground(isSelected ? new Color(0, 150, 150) : Color.WHITE);
                    label.setForeground(isSelected ? Color.WHITE : Color.BLACK);
                    label.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
                    return label;
                }
            });

            accountDropdown.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
                @Override protected JButton createArrowButton() {
                    JButton button = super.createArrowButton();
                    button.setBackground(Color.WHITE);
                    button.setBorder(BorderFactory.createLineBorder(new Color(0, 150, 150), 1));
                    return button;
                }
            });

            loadDefaultAccounts();

            rightPanel.add(accountLabel);
            rightPanel.add(accountDropdown);
            topPanel.add(rightPanel, BorderLayout.EAST);

            add(topPanel, BorderLayout.NORTH);

            String[] columnNames = {"Date", "Description", "Debit", "Credit", "Balance"};
            ledgerTableModel = new DefaultTableModel(columnNames, 0) {
                private static final long serialVersionUID = 1L;
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };

            ledgerTable = new JTable(ledgerTableModel);
            ledgerTable.setRowHeight(25);
            ledgerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            ledgerTable.setBackground(Color.WHITE);
            ledgerTable.setForeground(Color.BLACK);
            ledgerTable.setFillsViewportHeight(true);
            ledgerTable.setShowGrid(true);
            ledgerTable.setGridColor(new Color(0, 140, 140));
            ledgerTable.setIntercellSpacing(new Dimension(1, 1));

            ledgerTable.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(0, 140, 140)));

            ledgerTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 1L;
                @Override
                public Component getTableCellRendererComponent(
                        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                    JLabel cell = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                    cell.setHorizontalAlignment(SwingConstants.LEFT);
                    cell.setOpaque(true);
                    cell.setBackground(isSelected ? new Color(220, 250, 250) : Color.WHITE);
                    cell.setBorder(BorderFactory.createCompoundBorder(
                            cell.getBorder(),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)
                    ));
                    return cell;
                }
            });

            JTableHeader header = ledgerTable.getTableHeader();
            header.setBackground(new Color(0, 170, 170));
            header.setForeground(Color.WHITE);
            header.setFont(new Font("Segoe UI", Font.BOLD, 14));
            header.setReorderingAllowed(false);

            JScrollPane scrollPane = new JScrollPane(ledgerTable);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setBackground(Color.WHITE);

            customizeScrollBar(scrollPane.getVerticalScrollBar());
            customizeScrollBar(scrollPane.getHorizontalScrollBar());

            add(scrollPane, BorderLayout.CENTER);

            accountDropdown.addActionListener(e -> refreshLedgerTable());
            addBlankRows(ledgerTableModel, 30);
        }

        private void customizeScrollBar(JScrollBar bar) {
            bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
                private final Color THUMB_COLOR = new Color(0, 150, 150);
                private final Color TRACK_COLOR = new Color(240, 240, 240);

                @Override protected void configureScrollBarColors() {
                    this.thumbColor = THUMB_COLOR;
                    this.trackColor = TRACK_COLOR;
                }
                @Override protected JButton createDecreaseButton(int orientation) {
                    JButton button = super.createDecreaseButton(orientation);
                    button.setBackground(TRACK_COLOR);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    return button;
                }
                @Override protected JButton createIncreaseButton(int orientation) {
                    JButton button = super.createIncreaseButton(orientation);
                    button.setBackground(TRACK_COLOR);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    return button;
                }
            });
        }
        
        private void customizeComboPopupScrollBar(JComboBox<?> combo) {
            combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {

                    Object child = combo.getUI().getAccessibleChild(combo, 0);
                    if (!(child instanceof javax.swing.JPopupMenu)) return;

                    javax.swing.JPopupMenu popup = (javax.swing.JPopupMenu) child;

                    javax.swing.JScrollPane scroll = null;
                    for (Component c : popup.getComponents()) {
                        if (c instanceof javax.swing.JScrollPane) {
                            scroll = (javax.swing.JScrollPane) c;
                            break;
                        }
                    }

                    // SAFE CHECK to prevent NullPointerException
                    if (scroll != null) {
                        JScrollBar v = scroll.getVerticalScrollBar();
                        JScrollBar h = scroll.getHorizontalScrollBar();

                        if (v != null) customizeScrollBar(v);
                        if (h != null) customizeScrollBar(h);
                    }
                }

                @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
                @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
            });
        }


        private void addBlankRows(DefaultTableModel model, int count) {
            for (int i = 0; i < count; i++) {
                model.addRow(new Object[]{"", "", "", "", ""});
            }
        }

        private void loadDefaultAccounts() {
            String[][] allAccounts = {
                    {"Cash", "ASSET"},
                    {"Accounts Receivable", "ASSET"},
                    {"Inventory", "ASSET"},
                    {"Prepaid Expenses", "ASSET"},
                    {"Equipment", "ASSET"},
                    {"Accumulated Depreciation", "ASSET"},
                    {"Accounts Payable", "LIABILITY"},
                    {"Notes Payable", "LIABILITY"},
                    {"Owner's Capital", "EQUITY"},
                    {"Service Revenue", "INCOME"},
                    {"Sales Revenue", "INCOME"},
                    {"Rent Expense", "EXPENSE"},
                    {"Salaries Expense", "EXPENSE"},
                    {"Supplies Expense", "EXPENSE"},
                    {"Utilities Expense", "EXPENSE"},
                    {"Cost of Goods Sold", "EXPENSE"}
            };
            for (String[] acc : allAccounts) accountDropdown.addItem(acc[0]);
        }

        public void addTransactionEntry(Transaction t) {
            double debitAmount = t.getAmount();
            double creditAmount = 0;
            updateLedger(t.getDebitAccount(), t.getDate(), t.getDescription(), debitAmount, creditAmount);

            debitAmount = 0;
            creditAmount = t.getAmount();
            updateLedger(t.getCreditAccount(), t.getDate(), t.getDescription(), debitAmount, creditAmount);
        }

        public void updateLedger(String accountName, String date, String description, double debit, double credit) {
            ledgerData.computeIfAbsent(accountName, k -> new ArrayList<>())
                    .add(new LedgerEntry(date, description, debit, credit));
            refreshLedgerTable();
        }

        private void refreshLedgerTable() {
            ledgerTableModel.setRowCount(0);
            String selected = (String) accountDropdown.getSelectedItem();
            if (selected == null) return;
            java.util.List<LedgerEntry> entries = ledgerData.get(selected);

            double runningBalance = 0;

            if (entries != null) {
                for (LedgerEntry e : entries) {
                    runningBalance += e.debit - e.credit;
                    ledgerTableModel.addRow(new Object[]{
                            e.date,
                            e.description,
                            e.debit == 0 ? "" : String.format("%.2f", e.debit),
                            e.credit == 0 ? "" : String.format("%.2f", e.credit),
                            String.format("%.2f", runningBalance)
                    });
                }
            }
            addBlankRows(ledgerTableModel, 30);
        }

        private static class LedgerEntry {
            final String date; final String description; final double debit; final double credit;
            LedgerEntry(String date, String description, double debit, double credit) {
                this.date = date; this.description = description; this.debit = debit; this.credit = credit;
            }
        }
    }

    // WrapLayout
    static class WrapLayout extends FlowLayout {
        private static final long serialVersionUID = 1L;
        public WrapLayout() { super(); }
        public WrapLayout(int align) { super(align); }
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                Container container = target;

                while (container.getSize().width == 0 && container.getParent() != null)
                    container = container.getParent();

                if (targetWidth == 0) targetWidth = container.getSize().width;

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + hgap * 2);

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;

                for (Component m : target.getComponents()) {
                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                        if (rowWidth + d.width > maxWidth && rowWidth != 0) {
                            dim.width = Math.max(dim.width, rowWidth);
                            dim.height += rowHeight + vgap;
                            rowWidth = 0;
                            rowHeight = 0;
                        }
                        rowWidth += d.width + hgap;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }
                dim.width = Math.max(dim.width, rowWidth);
                dim.height += rowHeight + vgap;
                dim.width += insets.left + insets.right + hgap * 2;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }
    }
}