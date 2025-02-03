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
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            bookingService = (IBookingService) registry.lookup("BookingService");
            reservationService = (IReservationService) registry.lookup("ReservationService");
            cancellationService = (ICancellationService) registry.lookup("CancellationService");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error connecting to services: " + e.getMessage());
            System.exit(1);
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
        
        try {
            availableSeatsLabel = new JLabel("Available Seats: " + 
                reservationService.getAvailableSeats(), SwingConstants.CENTER);
        } catch (RemoteException e) {
            availableSeatsLabel = new JLabel("Error fetching seats", SwingConstants.CENTER);
        }
        availableSeatsLabel.setFont(HEADER_FONT);
        availableSeatsLabel.setForeground(PRIMARY_COLOR);

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(availableSeatsLabel, BorderLayout.CENTER);

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
        String[] columnNames = {"Booking ID", "Seats", "Amount", "Status"};
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
        
        // Set column widths
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(80);
        columnModel.getColumn(2).setPreferredWidth(100);
        columnModel.getColumn(3).setPreferredWidth(100);
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
            if (booking.status.equals("CONFIRMED")) {
                cancelBooking(booking);
            } else {
                showError("Only CONFIRMED bookings can be cancelled", "Cannot Cancel");
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
            model.addRow(new Object[]{
                booking.bookingId,
                booking.numSeats,
                "₹" + booking.amount,
                booking.status
            });
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

    private void cancelBooking(IIRCTCService.BookingDetails booking) {
        try {
            System.out.println("Attempting to cancel booking: " + booking.bookingId);
            boolean cancelled = cancellationService.cancelBooking(booking);
            if (cancelled) {
                booking.status = "CANCELLED"; // Update the status locally
                JOptionPane.showMessageDialog(this, 
                    "Booking cancelled successfully!\nRefund amount: ₹" + booking.amount);
                availableSeatsLabel.setText("Available Seats: " + 
                    reservationService.getAvailableSeats());
                updateBookingHistoryPanel();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Cancellation failed", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Add this for debugging
            JOptionPane.showMessageDialog(this, 
                "Cancellation failed: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new IRCTCMainFrame().setVisible(true);
        });
    }
} 