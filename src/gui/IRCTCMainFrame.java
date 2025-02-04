package gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import javax.swing.event.ListSelectionListener;
import java.awt.event.*;
import common.IIRCTCService;
import booking.IBookingService;
import reservation.IReservationService;
import cancellation.ICancellationService;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class IRCTCMainFrame extends JFrame {
    private IBookingService bookingService;
    private IReservationService reservationService;
    private ICancellationService cancellationService;
    private JLabel availableSeatsLabel;
    private JTextField numTicketsField;
    private List<IIRCTCService.BookingDetails> bookingHistory;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JTable bookingHistoryTable;
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210); // Material Blue
    private static final Color SECONDARY_COLOR = new Color(245, 245, 245); // Light Gray
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
    private static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font NORMAL_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    static {
        // Set up button UI
        UIManager.put("Button.select", PRIMARY_COLOR.darker());
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
    }

    public IRCTCMainFrame() {
        super("IRCTC Railway Reservation System");
        bookingHistory = new ArrayList<>();
        setUIFont(new FontUIResource("Segoe UI", Font.PLAIN, 14));
        initializeServices();
        setupGUI();
    }

    private void setUIFont(FontUIResource font) {
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, font);
            }
        }
    }

    private void initializeServices() {
        int maxRetries = 5;
        int retryDelay = 1000; // 1 second

        for (int i = 0; i < maxRetries; i++) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                bookingService = (IBookingService) registry.lookup("BookingService");
                reservationService = (IReservationService) registry.lookup("ReservationService");
                cancellationService = (ICancellationService) registry.lookup("CancellationService");
                
                // Test the connection
                int seats = reservationService.getAvailableSeats();
                System.out.println("Successfully connected to services. Available seats: " + seats);
                return;
                
            } catch (Exception e) {
                System.err.println("Attempt " + (i + 1) + " to connect to services failed: " + e.getMessage());
                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to connect to services after " + maxRetries + " attempts.\n" +
                        "Please make sure the server is running.",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
            }
        }
    }

    private void setupGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setBackground(SECONDARY_COLOR);

        // Create menu bar with modern look
        JMenuBar menuBar = createModernMenuBar();
        setJMenuBar(menuBar);

        // Create card layout
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(SECONDARY_COLOR);

        // Add panels
        cardPanel.add(createBookingPanel(), "booking");
        cardPanel.add(createBookingHistoryPanel(), "history");

        add(cardPanel);
        cardLayout.show(cardPanel, "booking");
    }

    private JMenuBar createModernMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(PRIMARY_COLOR);
        
        JMenu menu = new JMenu("Navigation");
        menu.setForeground(Color.WHITE);
        
        JMenuItem bookingItem = createMenuItem("Book Tickets", "booking");
        JMenuItem historyItem = createMenuItem("Booking History", "history");
        
        menu.add(bookingItem);
        menu.add(historyItem);
        menuBar.add(menu);
        return menuBar;
    }

    private JMenuItem createMenuItem(String text, String cardName) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(NORMAL_FONT);
        item.addActionListener(e -> {
            if (cardName.equals("history")) {
                updateBookingHistoryPanel();
            }
            showCard(cardName);
        });
        return item;
    }

    private JPanel createBookingPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(SECONDARY_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Railway Ticket Reservation", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(PRIMARY_COLOR);
        
        availableSeatsLabel = new JLabel("Fetching available seats...", SwingConstants.CENTER);
        availableSeatsLabel.setFont(HEADER_FONT);
        availableSeatsLabel.setForeground(PRIMARY_COLOR);

        // Create a refresh button for seats
        JButton refreshSeatsButton = createStyledButton("↻");
        refreshSeatsButton.setToolTipText("Refresh Available Seats");
        refreshSeatsButton.addActionListener(e -> updateAvailableSeats());

        JPanel seatsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        seatsPanel.setOpaque(false);
        seatsPanel.add(availableSeatsLabel);
        seatsPanel.add(refreshSeatsButton);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(seatsPanel, BorderLayout.CENTER);

        // Booking Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel ticketsLabel = new JLabel("Number of Tickets:");
        ticketsLabel.setFont(NORMAL_FONT);
        numTicketsField = new JTextField(10);
        numTicketsField.setFont(NORMAL_FONT);
        
        JButton bookButton = createStyledButton("Book Tickets");
        bookButton.addActionListener(e -> bookTickets());

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(ticketsLabel, gbc);
        gbc.gridx = 1;
        formPanel.add(numTicketsField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        formPanel.add(bookButton, gbc);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Update seats initially
        updateAvailableSeats();

        return mainPanel;
    }

    private JPanel createBookingHistoryPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBackground(SECONDARY_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Header
        JLabel titleLabel = new JLabel("Booking History", SwingConstants.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setForeground(PRIMARY_COLOR);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"Booking ID", "Tickets (Current/Total)", "Amount", "Status"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        bookingHistoryTable = new JTable(model);
        setupTable(bookingHistoryTable);
        
        JScrollPane scrollPane = new JScrollPane(bookingHistoryTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        JButton cancelButton = createStyledButton("Cancel Selected Booking");
        cancelButton.addActionListener(e -> handleCancellation());

        JButton refreshButton = createStyledButton("Refresh");
        refreshButton.addActionListener(e -> updateBookingHistoryPanel());

        buttonPanel.add(cancelButton);
        buttonPanel.add(refreshButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void setupTable(JTable table) {
        table.setFont(NORMAL_FONT);
        table.setRowHeight(30);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(HEADER_FONT);
        table.getTableHeader().setBackground(PRIMARY_COLOR);
        table.getTableHeader().setForeground(Color.WHITE);
        
        // Update column names
        table.getColumnModel().getColumn(1).setHeaderValue("Tickets (Current/Total)");
        
        // Set column widths
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);  // Booking ID
        columnModel.getColumn(1).setPreferredWidth(150);  // Tickets
        columnModel.getColumn(2).setPreferredWidth(100);  // Amount
        columnModel.getColumn(3).setPreferredWidth(150);  // Status
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(NORMAL_FONT);
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    private void handleCancellation() {
        int selectedRow = bookingHistoryTable.getSelectedRow();
        if (selectedRow != -1) {
            IIRCTCService.BookingDetails booking = bookingHistory.get(selectedRow);
            // Allow cancellation for both CONFIRMED and PARTIALLY CANCELLED tickets
            if (booking.status.equals("CONFIRMED") || booking.status.equals("PARTIALLY CANCELLED")) {
                String input = JOptionPane.showInputDialog(this,
                    "Enter number of tickets to cancel (1-" + booking.numSeats + "):",
                    "Cancel Tickets",
                    JOptionPane.QUESTION_MESSAGE);
                
                if (input == null) {
                    return; // User clicked Cancel
                }
                
                try {
                    int numTicketsToCancel = Integer.parseInt(input);
                    if (numTicketsToCancel > 0 && numTicketsToCancel <= booking.numSeats) {
                        cancelBooking(booking, numTicketsToCancel);
                    } else {
                        showError("Please enter a valid number between 1 and " + booking.numSeats, "Invalid Input");
                    }
                } catch (NumberFormatException e) {
                    showError("Please enter a valid number", "Invalid Input");
                }
            } else {
                showError("Only CONFIRMED or PARTIALLY CANCELLED bookings can be cancelled", "Cannot Cancel");
            }
        } else {
            showError("Please select a booking to cancel", "No Selection");
        }
    }

    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.WARNING_MESSAGE);
    }

    private void updateBookingHistoryPanel() {
        DefaultTableModel model = (DefaultTableModel) bookingHistoryTable.getModel();
        model.setRowCount(0); // Clear existing rows

        for (IIRCTCService.BookingDetails booking : bookingHistory) {
            String status = booking.status;
            String ticketInfo = booking.numSeats + " / " + getOriginalTicketCount(booking);
            String amount = "₹" + String.format("%.2f", booking.amount);

            model.addRow(new Object[]{
                booking.bookingId,
                ticketInfo,
                amount,
                status
            });
            
            System.out.println("Updating booking " + booking.bookingId + 
                             ": Tickets=" + ticketInfo + 
                             ", Status=" + status);
        }
        
        // Select first row if available
        if (model.getRowCount() > 0) {
            bookingHistoryTable.setRowSelectionInterval(0, 0);
        }
    }

    private void showCard(String cardName) {
        cardLayout.show(cardPanel, cardName);
    }

    private void bookTickets() {
        try {
            int numTickets = Integer.parseInt(numTicketsField.getText());
            if (numTickets <= 0) {
                throw new NumberFormatException();
            }
            
            IIRCTCService.BookingDetails booking = bookingService.bookTicket(numTickets);
            originalTicketCounts.put(booking.bookingId, numTickets); // Store original count
            bookingHistory.add(booking);
            
            if (booking.status.equals("CONFIRMED")) {
                JOptionPane.showMessageDialog(this, 
                    "Booking Successful!\nBooking ID: " + booking.bookingId +
                    "\nAmount: ₹" + booking.amount);
                availableSeatsLabel.setText("Available Seats: " + 
                    reservationService.getAvailableSeats());
                numTicketsField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Booking failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid number of tickets", 
                "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Booking failed: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelBooking(IIRCTCService.BookingDetails booking, int numTicketsToCancel) {
        try {
            debugBooking(booking, "Before cancellation"); // Add debug information
            System.out.println("Attempting to cancel " + numTicketsToCancel + " tickets from booking: " + booking.bookingId);
            
            boolean cancelled = cancellationService.cancelBooking(booking, numTicketsToCancel);
            if (cancelled) {
                // Calculate refund amount before updating the booking
                double refundAmount = (booking.amount / booking.numSeats) * numTicketsToCancel;
                
                // Update the booking details
                booking.numSeats -= numTicketsToCancel;
                booking.amount -= refundAmount;
                
                // Update status
                if (booking.numSeats == 0) {
                    booking.status = "CANCELLED";
                } else {
                    booking.status = "PARTIALLY CANCELLED";
                }

                debugBooking(booking, "After cancellation"); // Add debug information

                JOptionPane.showMessageDialog(this, 
                    "Cancellation successful!\n" +
                    "Number of tickets cancelled: " + numTicketsToCancel + "\n" +
                    "Remaining tickets: " + booking.numSeats + "\n" +
                    "Refund amount: ₹" + String.format("%.2f", refundAmount));
                    
                // Update UI
                updateAvailableSeats();
                updateBookingHistoryPanel();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Cancellation failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Cancellation failed: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Add this method to track original ticket count
    private Map<Integer, Integer> originalTicketCounts = new HashMap<>();

    private int getOriginalTicketCount(IIRCTCService.BookingDetails booking) {
        return originalTicketCounts.getOrDefault(booking.bookingId, booking.numSeats);
    }

    private void updateAvailableSeats() {
        try {
            int seats = reservationService.getAvailableSeats();
            availableSeatsLabel.setText("Available Seats: " + seats);
            availableSeatsLabel.setForeground(PRIMARY_COLOR);
        } catch (Exception e) {
            availableSeatsLabel.setText("Error fetching seats");
            availableSeatsLabel.setForeground(Color.RED);
            System.err.println("Error fetching seats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void debugBooking(IIRCTCService.BookingDetails booking, String operation) {
        System.out.println(operation + " - Booking ID: " + booking.bookingId);
        System.out.println("Current tickets: " + booking.numSeats);
        System.out.println("Original tickets: " + getOriginalTicketCount(booking));
        System.out.println("Status: " + booking.status);
        System.out.println("Amount: " + booking.amount);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IRCTCMainFrame().setVisible(true);
        });
    }
} 