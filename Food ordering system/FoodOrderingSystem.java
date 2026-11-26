import java.util.*;

/** Online Food Ordering System: ArrayList (menu/cart), Queue (orders), Stack (undo/history), Sorting (menu). */
class MenuItem {
    int id; String name, category; double price, rating;
    MenuItem(int id, String name, String category, double price, double rating) {
        this.id = id; this.name = name; this.category = category; this.price = price; this.rating = rating;
    }
    public String toString() { return String.format("%-4d %-22s %-12s Rs.%-8.2f %.1f*", id, name, category, price, rating); }
}

class CartItem {
    MenuItem item; int qty;
    CartItem(MenuItem item, int qty) { this.item = item; this.qty = qty; }
    double total() { return item.price * qty; }
}

class Order {
    static int counter = 1000;
    int orderId; String customer, status = "PLACED"; ArrayList<CartItem> items; double total;
    Order(String customer, ArrayList<CartItem> items) {
        this.orderId = ++counter; this.customer = customer; this.items = items;
        for (CartItem c : items) total += c.total();
    }
    void print() {
        System.out.println("Order #" + orderId + " | " + customer + " | " + status);
        for (CartItem c : items) System.out.printf("   %-22s x%-3d Rs.%.2f%n", c.item.name, c.qty, c.total());
        System.out.printf("   TOTAL: Rs.%.2f%n", total);
    }
}

class CartAction {   // recorded on the undo Stack
    enum Type { ADD, REMOVE }
    Type type; MenuItem item; int qty;
    CartAction(Type type, MenuItem item, int qty) { this.type = type; this.item = item; this.qty = qty; }
}

public class FoodOrderingSystem {
    ArrayList<MenuItem> menu = new ArrayList<>();
    ArrayList<CartItem> cart = new ArrayList<>();
    Stack<CartAction> undoStack = new Stack<>();
    Queue<Order> pendingOrders = new LinkedList<>();
    Stack<Order> orderHistory = new Stack<>();
    Scanner sc = new Scanner(System.in);
    String customerName = "Guest";

    FoodOrderingSystem() {
        String[][] d = {{"Paneer Butter Masala", "Main Course", "220", "4.5"}, {"Veg Biryani", "Main Course", "180", "4.2"},
            {"Butter Naan", "Bread", "35", "4.0"}, {"Masala Dosa", "South Indian", "90", "4.6"}, {"Veg Burger", "Fast Food", "120", "3.9"},
            {"French Fries", "Fast Food", "80", "4.1"}, {"Gulab Jamun", "Dessert", "60", "4.7"}, {"Cold Coffee", "Beverage", "70", "4.3"},
            {"Masala Chai", "Beverage", "20", "4.4"}};
        for (int i = 0; i < d.length; i++)
            menu.add(new MenuItem(i + 1, d[i][0], d[i][1], Double.parseDouble(d[i][2]), Double.parseDouble(d[i][3])));
    }

    // ---- input helpers ----
    double num(String p) {
        while (true) {
            System.out.print(p);
            try { return Double.parseDouble(sc.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("Please enter a valid number."); }
        }
    }
    int readInt(String p) { return (int) num(p); }
    String text(String p) { System.out.print(p); return sc.nextLine().trim(); }

    // ---- menu & sorting ----
    void show(List<MenuItem> l) {
        System.out.printf("%n%-4s %-22s %-12s %-11s %s%n", "ID", "Name", "Category", "Price", "Rating");
        System.out.println("--------------------------------------------------------------");
        if (l.isEmpty()) System.out.println("No items found.");
        for (MenuItem m : l) System.out.println(m);
    }

    void sortMenu() {
        System.out.println("\nSort by: 1) Price Low->High  2) Price High->Low  3) Name A-Z  4) Rating High->Low");
        int ch = readInt("Choice: ");
        ArrayList<MenuItem> l = new ArrayList<>(menu);          // sort a copy, keep original order
        if (ch == 1 || ch == 2) {                                // manual insertion sort on price
            for (int i = 1; i < l.size(); i++) {
                MenuItem k = l.get(i); int j = i - 1;
                while (j >= 0 && (ch == 1 ? l.get(j).price > k.price : l.get(j).price < k.price)) { l.set(j + 1, l.get(j)); j--; }
                l.set(j + 1, k);
            }
        } else if (ch == 3) l.sort(Comparator.comparing(m -> m.name.toLowerCase()));
        else if (ch == 4) l.sort((a, b) -> Double.compare(b.rating, a.rating));
        else { System.out.println("Invalid choice."); return; }
        show(l);
    }

    void filterByCategory() {
        String cat = text("Enter category (e.g. Fast Food, Dessert, Beverage): ");
        ArrayList<MenuItem> r = new ArrayList<>();
        for (MenuItem m : menu) if (m.category.equalsIgnoreCase(cat)) r.add(m);
        show(r);
    }

    // ---- cart ----
    MenuItem findMenu(int id) { for (MenuItem m : menu) if (m.id == id) return m; return null; }
    CartItem findCart(int id) { for (CartItem c : cart) if (c.item.id == id) return c; return null; }

    void change(MenuItem m, int d) {                             // d > 0 adds, d < 0 removes
        CartItem c = findCart(m.id);
        if (c == null) cart.add(new CartItem(m, d));
        else { c.qty += d; if (c.qty <= 0) cart.remove(c); }
    }

    void addToCart() {
        MenuItem m = findMenu(readInt("Enter item ID: "));
        if (m == null) { System.out.println("Item not found."); return; }
        int q = readInt("Quantity: ");
        if (q <= 0) { System.out.println("Quantity must be positive."); return; }
        change(m, q);
        undoStack.push(new CartAction(CartAction.Type.ADD, m, q));
        System.out.println(q + " x " + m.name + " added to cart.");
    }

    void removeFromCart() {
        CartItem c = findCart(readInt("Enter item ID to remove: "));
        if (c == null) { System.out.println("Item is not in your cart."); return; }
        int q = readInt("Quantity to remove (max " + c.qty + "): ");
        if (q <= 0 || q > c.qty) { System.out.println("Invalid quantity."); return; }
        MenuItem m = c.item;
        change(m, -q);
        undoStack.push(new CartAction(CartAction.Type.REMOVE, m, q));
        System.out.println("Removed " + q + " x " + m.name);
    }

    void undoLastAction() {
        if (undoStack.isEmpty()) { System.out.println("Nothing to undo."); return; }
        CartAction a = undoStack.pop();                          // LIFO
        boolean wasAdd = a.type == CartAction.Type.ADD;
        change(a.item, wasAdd ? -a.qty : a.qty);
        System.out.println("Undone: " + (wasAdd ? "addition" : "removal") + " of " + a.qty + " x " + a.item.name);
    }

    void viewCart() {
        if (cart.isEmpty()) { System.out.println("\nYour cart is empty."); return; }
        System.out.println("\n--- Your Cart ---");
        double t = 0;
        for (CartItem c : cart) { System.out.printf("%-22s x%-3d Rs.%.2f%n", c.item.name, c.qty, c.total()); t += c.total(); }
        System.out.printf("Total: Rs.%.2f%n", t);
    }

    // ---- orders ----
    void placeOrder() {
        if (cart.isEmpty()) { System.out.println("Cart is empty. Add items first."); return; }
        viewCart();
        if (!text("Confirm order? (y/n): ").equalsIgnoreCase("y")) { System.out.println("Order cancelled."); return; }
        Order o = new Order(customerName, new ArrayList<>(cart));
        pendingOrders.offer(o);                                  // enqueue
        cart.clear(); undoStack.clear();
        System.out.println("Order placed! Your Order ID: " + o.orderId + " | Orders ahead of you: " + (pendingOrders.size() - 1));
    }

    void processNextOrder() {
        Order o = pendingOrders.poll();                          // dequeue (FIFO)
        if (o == null) { System.out.println("No pending orders."); return; }
        o.status = "DELIVERED";
        orderHistory.push(o);
        System.out.println("Processed and delivered:");
        o.print();
    }

    void viewPendingOrders() {
        if (pendingOrders.isEmpty()) { System.out.println("No pending orders."); return; }
        System.out.println("\n--- Pending Orders (in serving order) ---");
        int p = 1;
        for (Order o : pendingOrders) System.out.printf("%d) Order #%d | %s | Rs.%.2f%n", p++, o.orderId, o.customer, o.total);
    }

    void viewOrderHistory() {
        if (orderHistory.isEmpty()) { System.out.println("No completed orders yet."); return; }
        System.out.println("\n--- Order History (latest first) ---");
        for (int i = orderHistory.size() - 1; i >= 0; i--) orderHistory.get(i).print();
    }

    void addMenuItem() {
        String n = text("Name: "), c = text("Category: ");
        double p = num("Price: "), r = num("Rating (0-5): ");
        int id = menu.get(menu.size() - 1).id + 1;
        menu.add(new MenuItem(id, n, c, p, r));
        System.out.println("Menu item added with ID " + id);
    }

    void run() {
        System.out.println("=== Welcome to the Online Food Ordering System ===");
        String n = text("Enter your name: ");
        if (!n.isEmpty()) customerName = n;
        while (true) {
            System.out.println("\n========== MAIN MENU ==========\n 1. View menu\n 2. Sort menu\n 3. Filter by category\n"
                + " 4. Add item to cart\n 5. Remove item from cart\n 6. Undo last cart action\n 7. View cart\n 8. Place order\n"
                + " 9. View my order history\n--------- Restaurant / Admin ---------\n10. View pending orders (queue)\n"
                + "11. Process next order\n12. Add new menu item\n 0. Exit");
            switch (readInt("Choice: ")) {
                case 1: show(menu); break;
                case 2: sortMenu(); break;
                case 3: filterByCategory(); break;
                case 4: addToCart(); break;
                case 5: removeFromCart(); break;
                case 6: undoLastAction(); break;
                case 7: viewCart(); break;
                case 8: placeOrder(); break;
                case 9: viewOrderHistory(); break;
                case 10: viewPendingOrders(); break;
                case 11: processNextOrder(); break;
                case 12: addMenuItem(); break;
                case 0: System.out.println("Thank you! Goodbye."); return;
                default: System.out.println("Invalid choice. Try again.");
            }
        }
    }

    public static void main(String[] args) { new FoodOrderingSystem().run(); }
}
