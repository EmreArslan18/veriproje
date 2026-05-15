import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// ═══════════════════════════════════════════════════════════════════
//  AKILLI PAKETLEME VE SIRALI KARGO YÖNETİM SİSTEMİ - BACKEND
//  Veri Yapıları: AVL Ağacı, Stack, Priority Queue (Min-Heap), Graf (Dijkstra), Deque
// ═══════════════════════════════════════════════════════════════════
public class CargoBackend {

    // ─────────────────────────────────────────
    // ENUM'LAR
    // ─────────────────────────────────────────
    public enum CargoStatus {
        BEKLEMEDE("Beklemede"),
        PAKETLENDI("Paketlendi"),
        DAGITIMDA("Dağıtımda"),
        TESLIM_EDILDI("Teslim Edildi"),
        IPTAL("İptal");

        public final String label;
        CargoStatus(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum Priority {
        ACIL(1, "Acil"),
        YUKSEK(2, "Yüksek"),
        NORMAL(3, "Normal"),
        DUSUK(4, "Düşük");

        public final int value;
        public final String label;
        Priority(int value, String label) { this.value = value; this.label = label; }
        @Override public String toString() { return label; }
    }

    // ─────────────────────────────────────────
    // KARGO MODELİ
    // ─────────────────────────────────────────
    public static class Cargo implements Comparable<Cargo> {
        static int counter = 1;

        public final String id;
        public String sender, receiver, destination;
        public double weight, volume, value;
        public Priority priority;
        public CargoStatus status;
        public LocalDateTime createdAt, updatedAt;
        public String packageId;

        public Cargo(String sender, String receiver, double weight, double volume,
                     Priority priority, String destination, double value) {
            this.id          = String.format("KRG-%04d", counter++);
            this.sender      = sender;
            this.receiver    = receiver;
            this.weight      = weight;
            this.volume      = volume;
            this.priority    = priority;
            this.destination = destination;
            this.value       = value;
            this.status      = CargoStatus.BEKLEMEDE;
            this.createdAt   = LocalDateTime.now();
            this.updatedAt   = LocalDateTime.now();
        }

        @Override public int compareTo(Cargo o) {
            return Integer.compare(this.priority.value, o.priority.value);
        }

        @Override public String toString() { return id; }
    }

    // ─────────────────────────────────────────
    // PAKET MODELİ
    // ─────────────────────────────────────────
    public static class Package {
        static int counter = 1;

        public final String id;
        public final List<Cargo> cargos = new ArrayList<>();
        public final double maxWeight, maxVolume;
        public final LocalDateTime createdAt;

        public Package(double maxWeight, double maxVolume) {
            this.id        = String.format("PKT-%04d", counter++);
            this.maxWeight = maxWeight;
            this.maxVolume = maxVolume;
            this.createdAt = LocalDateTime.now();
        }

        public double currentWeight() { return cargos.stream().mapToDouble(c -> c.weight).sum(); }
        public double currentVolume() { return cargos.stream().mapToDouble(c -> c.volume).sum(); }

        public double utilization() {
            double w = currentWeight() / maxWeight;
            double v = currentVolume() / maxVolume;
            return Math.round(Math.max(w, v) * 1000.0) / 10.0;
        }

        public boolean canFit(Cargo c) {
            return currentWeight() + c.weight <= maxWeight &&
                   currentVolume() + c.volume <= maxVolume;
        }

        public boolean addCargo(Cargo c) {
            if (!canFit(c)) return false;
            cargos.add(c);
            c.packageId = id;
            c.status    = CargoStatus.PAKETLENDI;
            c.updatedAt = LocalDateTime.now();
            return true;
        }
    }

    // ─────────────────────────────────────────
    // AVL AĞACI – Kargo kayıt / ID'ye göre arama
    // ─────────────────────────────────────────
    public static class AVLTree {

        static class Node {
            Cargo cargo;
            String key;
            Node left, right;
            int height = 1;
            Node(Cargo c) { cargo = c; key = c.id; }
        }

        private Node root;
        private int size;

        private int height(Node n)  { return n == null ? 0 : n.height; }
        private int balance(Node n) { return n == null ? 0 : height(n.left) - height(n.right); }

        private void updateHeight(Node n) {
            n.height = 1 + Math.max(height(n.left), height(n.right));
        }

        private Node rotateRight(Node y) {
            Node x = y.left, T2 = x.right;
            x.right = y; y.left = T2;
            updateHeight(y); updateHeight(x);
            return x;
        }

        private Node rotateLeft(Node x) {
            Node y = x.right, T2 = y.left;
            y.left = x; x.right = T2;
            updateHeight(x); updateHeight(y);
            return y;
        }

        private Node insert(Node node, Cargo cargo) {
            if (node == null) { size++; return new Node(cargo); }
            int cmp = cargo.id.compareTo(node.key);
            if      (cmp < 0) node.left  = insert(node.left,  cargo);
            else if (cmp > 0) node.right = insert(node.right, cargo);
            else { node.cargo = cargo; return node; }

            updateHeight(node);
            int bf = balance(node);

            if (bf > 1  && cargo.id.compareTo(node.left.key)  < 0) return rotateRight(node);
            if (bf < -1 && cargo.id.compareTo(node.right.key) > 0) return rotateLeft(node);
            if (bf > 1  && cargo.id.compareTo(node.left.key)  > 0) { node.left  = rotateLeft(node.left);  return rotateRight(node); }
            if (bf < -1 && cargo.id.compareTo(node.right.key) < 0) { node.right = rotateRight(node.right); return rotateLeft(node); }
            return node;
        }

        public void insert(Cargo cargo) { root = insert(root, cargo); }

        public Cargo search(String id) {
            Node n = root;
            while (n != null) {
                int cmp = id.compareTo(n.key);
                if      (cmp == 0) return n.cargo;
                else if (cmp < 0)  n = n.left;
                else               n = n.right;
            }
            return null;
        }

        private void inorder(Node n, List<Cargo> result) {
            if (n == null) return;
            inorder(n.left, result);
            result.add(n.cargo);
            inorder(n.right, result);
        }

        public List<Cargo> inorder() {
            List<Cargo> r = new ArrayList<>();
            inorder(root, r);
            return r;
        }

        public int treeHeight() { return height(root); }
        public int size()       { return size; }
    }

    // ─────────────────────────────────────────
    // STACK – İşlem geçmişi / geri al (LIFO)
    // ─────────────────────────────────────────
    public static class ActionStack {

        public static class Action {
            public String type, cargoId, detail;
            public LocalDateTime timestamp;

            public Action(String type, String cargoId, String detail) {
                this.type      = type;
                this.cargoId   = cargoId;
                this.detail    = detail;
                this.timestamp = LocalDateTime.now();
            }

            public String formattedTime() {
                return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        }

        private final Deque<Action> stack = new ArrayDeque<>();
        private final int maxSize;

        public ActionStack(int maxSize) { this.maxSize = maxSize; }

        public void push(Action a) {
            if (stack.size() >= maxSize) {
                // En alttan çıkar
                List<Action> list = new ArrayList<>(stack);
                stack.clear();
                for (int i = 1; i < list.size(); i++) stack.addLast(list.get(i));
            }
            stack.push(a);
        }

        public Action pop()      { return stack.isEmpty() ? null : stack.pop(); }
        public Action peek()     { return stack.isEmpty() ? null : stack.peek(); }
        public boolean isEmpty() { return stack.isEmpty(); }
        public int size()        { return stack.size(); }

        public List<Action> history() { return new ArrayList<>(stack); }
    }

    // ─────────────────────────────────────────
    // ÖNCELİK KUYRUĞU – Min-Heap (Java PriorityQueue)
    // ─────────────────────────────────────────
    public static class CargoPriorityQueue {

        private final java.util.PriorityQueue<Cargo> heap;

        public CargoPriorityQueue() {
            heap = new java.util.PriorityQueue<>(
                Comparator.comparingInt((Cargo c) -> c.priority.value)
                          .thenComparing(c -> c.createdAt)
            );
        }

        public void enqueue(Cargo c)  { heap.offer(c); }

        public Cargo dequeue() {
            while (!heap.isEmpty()) {
                Cargo c = heap.poll();
                if (c.status == CargoStatus.BEKLEMEDE) return c;
            }
            return null;
        }

        public Cargo peek() {
            for (Cargo c : heap)
                if (c.status == CargoStatus.BEKLEMEDE) return c;
            return null;
        }

        public List<Cargo> allItems() {
            return heap.stream()
                .filter(c -> c.status == CargoStatus.BEKLEMEDE)
                .sorted(Comparator.comparingInt((Cargo c) -> c.priority.value))
                .collect(Collectors.toList());
        }

        public int size() {
            return (int) heap.stream()
                .filter(c -> c.status == CargoStatus.BEKLEMEDE).count();
        }
    }

    // ─────────────────────────────────────────
    // DEQUE – Paketleme hattı tamponu
    // ─────────────────────────────────────────
    public static class PackagingBuffer {

        private final Deque<Cargo> buf;
        private final int maxSize;

        public PackagingBuffer(int maxSize) {
            this.buf     = new ArrayDeque<>();
            this.maxSize = maxSize;
        }

        public void addUrgent(Cargo c) { if (buf.size() < maxSize) buf.addFirst(c); }
        public void addNormal(Cargo c) { if (buf.size() < maxSize) buf.addLast(c); }
        public Cargo getNext()         { return buf.isEmpty() ? null : buf.pollFirst(); }
        public int size()              { return buf.size(); }
        public List<Cargo> items()     { return new ArrayList<>(buf); }
    }

    // ─────────────────────────────────────────
    // DAĞITIM GRAFI – Dijkstra en kısa / hızlı rota
    // ─────────────────────────────────────────
    public static class DistributionGraph {

        public static class Edge {
            public final String to;
            public final double distance, timeHours;
            public Edge(String to, double d, double t) { this.to = to; distance = d; timeHours = t; }
        }

        public static class RouteResult {
            public final double cost;
            public final List<String> path;
            public RouteResult(double cost, List<String> path) { this.cost = cost; this.path = path; }
        }

        private final Map<String, List<Edge>> adj = new HashMap<>();
        public  final Set<String> cities          = new LinkedHashSet<>();

        public void addRoute(String a, String b, double dist, double time) {
            adj.computeIfAbsent(a, k -> new ArrayList<>()).add(new Edge(b, dist, time));
            adj.computeIfAbsent(b, k -> new ArrayList<>()).add(new Edge(a, dist, time));
            cities.add(a);
            cities.add(b);
        }

        /** byTime=true → en hızlı rota (saat),  byTime=false → en kısa rota (km) */
        public RouteResult dijkstra(String src, String dst, boolean byTime) {
            Map<String, Double> dist = new HashMap<>();
            Map<String, String> prev = new HashMap<>();
            cities.forEach(c -> dist.put(c, Double.MAX_VALUE));
            dist.put(src, 0.0);

            java.util.PriorityQueue<String> pq = new java.util.PriorityQueue<>(
                Comparator.comparingDouble(dist::get));
            pq.offer(src);

            while (!pq.isEmpty()) {
                String u = pq.poll();
                for (Edge e : adj.getOrDefault(u, List.of())) {
                    double w  = byTime ? e.timeHours : e.distance;
                    double nd = dist.get(u) + w;
                    if (nd < dist.getOrDefault(e.to, Double.MAX_VALUE)) {
                        dist.put(e.to, nd);
                        prev.put(e.to, u);
                        pq.offer(e.to);
                    }
                }
            }

            List<String> path = new ArrayList<>();
            String node = dst;
            while (node != null) { path.add(0, node); node = prev.get(node); }
            return new RouteResult(dist.getOrDefault(dst, -1.0), path);
        }

        public List<Edge> getNeighbors(String city) {
            return adj.getOrDefault(city, List.of());
        }

        /** Tüm kenarları tekrarsız döndürür: [şehirA, şehirB, km, saat] */
        public List<String[]> allRoutes() {
            Set<String> seen   = new HashSet<>();
            List<String[]> res = new ArrayList<>();
            for (Map.Entry<String, List<Edge>> e : adj.entrySet()) {
                for (Edge edge : e.getValue()) {
                    String key = e.getKey().compareTo(edge.to) < 0
                        ? e.getKey() + "|" + edge.to
                        : edge.to   + "|" + e.getKey();
                    if (seen.add(key))
                        res.add(new String[]{e.getKey(), edge.to,
                            String.valueOf((int) edge.distance),
                            String.valueOf(edge.timeHours)});
                }
            }
            return res;
        }
    }

    // ═══════════════════════════════════════════
    //  ANA SİSTEM – tüm veri yapılarını yönetir
    // ═══════════════════════════════════════════
    public static class CargoManagementSystem {

        public final AVLTree              avl    = new AVLTree();
        public final CargoPriorityQueue   queue  = new CargoPriorityQueue();
        public final ActionStack          stack  = new ActionStack(100);
        public final PackagingBuffer      buffer = new PackagingBuffer(20);
        public final DistributionGraph    graph  = new DistributionGraph();

        public final Map<String, Cargo>   cargos   = new LinkedHashMap<>();
        public final Map<String, Package> packages = new LinkedHashMap<>();

        public CargoManagementSystem() {
            buildNetwork();
        }

        // ── Ağ kurulumu ──────────────────────
        private void buildNetwork() {
            Object[][] routes = {
                {"Istanbul",  "Ankara",    450, 5.0},
                {"Istanbul",  "Bursa",     160, 2.5},
                {"Istanbul",  "Izmir",     570, 7.0},
                {"Ankara",    "Izmir",     590, 7.5},
                {"Ankara",    "Konya",     260, 3.5},
                {"Ankara",    "Samsun",    420, 5.5},
                {"Izmir",     "Antalya",   480, 6.0},
                {"Izmir",     "Mugla",     260, 3.5},
                {"Antalya",   "Konya",     210, 3.0},
                {"Bursa",     "Eskisehir", 150, 2.0},
                {"Eskisehir", "Ankara",    235, 3.0},
                {"Samsun",    "Trabzon",   340, 4.5},
                {"Konya",     "Adana",     330, 4.5},
                {"Adana",     "Gaziantep", 190, 2.5},
            };
            for (Object[] r : routes)
                graph.addRoute((String)r[0], (String)r[1],
                    ((Number)r[2]).doubleValue(), ((Number)r[3]).doubleValue());
        }

        // ── Kargo ekle ───────────────────────
        public Cargo addCargo(String sender, String receiver, double weight, double volume,
                              Priority priority, String destination, double value) {
            Cargo c = new Cargo(sender, receiver, weight, volume, priority, destination, value);
            avl.insert(c);
            queue.enqueue(c);
            cargos.put(c.id, c);
            stack.push(new ActionStack.Action("KARGO_EKLE", c.id,
                c.id + " eklendi (" + priority.label + ")"));
            return c;
        }

        // ── Kargo iptal ──────────────────────
        public boolean cancelCargo(String id) {
            Cargo c = avl.search(id);
            if (c == null || c.status == CargoStatus.TESLIM_EDILDI) return false;
            c.status    = CargoStatus.IPTAL;
            c.updatedAt = LocalDateTime.now();
            stack.push(new ActionStack.Action("IPTAL", id, id + " iptal edildi"));
            return true;
        }

        // ── Otomatik paketleme (FFD) ─────────
        public List<Package> autoPackage() {
            List<Cargo> waiting = cargos.values().stream()
                .filter(c -> c.status == CargoStatus.BEKLEMEDE)
                .sorted(Comparator.comparingInt((Cargo c) -> c.priority.value)
                    .thenComparingDouble((Cargo c) -> -c.weight))
                .collect(Collectors.toList());

            List<Package> newPkgs = new ArrayList<>();
            List<Package> open    = new ArrayList<>();

            for (Cargo cargo : waiting) {
                boolean placed = false;
                for (Package pkg : open) {
                    if (pkg.addCargo(cargo)) { placed = true; break; }
                }
                if (!placed) {
                    Package pkg = new Package(30, 50);
                    if (pkg.addCargo(cargo)) {
                        open.add(pkg);
                        newPkgs.add(pkg);
                        packages.put(pkg.id, pkg);
                    }
                }
            }
            stack.push(new ActionStack.Action("OTO_PAKET", null,
                newPkgs.size() + " yeni paket oluşturuldu"));
            return newPkgs;
        }

        // ── Manuel paketleme ─────────────────
        public boolean packageCargo(String cargoId, String pkgId) {
            Cargo c = avl.search(cargoId);
            if (c == null || c.status != CargoStatus.BEKLEMEDE) return false;

            Package pkg;
            if (pkgId != null) {
                pkg = packages.get(pkgId);
                if (pkg == null) return false;
            } else {
                pkg = new Package(30, 50);
                packages.put(pkg.id, pkg);
            }

            boolean ok = pkg.addCargo(c);
            if (ok) stack.push(new ActionStack.Action("PAKETLEME", cargoId,
                cargoId + " → " + pkg.id));
            return ok;
        }

        // ── Kargo teslim et ──────────────────
        public boolean deliverCargo(String id) {
            Cargo c = avl.search(id);
            if (c == null || c.status != CargoStatus.DAGITIMDA) return false;
            c.status    = CargoStatus.TESLIM_EDILDI;
            c.updatedAt = LocalDateTime.now();
            stack.push(new ActionStack.Action("TESLİM", id, id + " teslim edildi"));
            return true;
        }

        // ── Paketi sevkiyata ver ──────────────
        public boolean dispatchPackage(String pkgId) {
            Package pkg = packages.get(pkgId);
            if (pkg == null) return false;
            pkg.cargos.forEach(c -> {
                c.status    = CargoStatus.DAGITIMDA;
                c.updatedAt = LocalDateTime.now();
            });
            stack.push(new ActionStack.Action("SEVKİYAT", null,
                pkgId + " sevkiyata verildi"));
            return true;
        }

        // ── Son işlemi geri al ───────────────
        public ActionStack.Action undoLast() {
            ActionStack.Action a = stack.pop();
            if (a != null && "KARGO_EKLE".equals(a.type)) {
                Cargo c = avl.search(a.cargoId);
                if (c != null && c.status == CargoStatus.BEKLEMEDE)
                    c.status = CargoStatus.IPTAL;
            }
            return a;
        }

        // ── Dashboard istatistikleri ──────────
        public Map<String, Object> dashboardStats() {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("toplam",       cargos.size());
            s.put("bekleyen",     count(CargoStatus.BEKLEMEDE));
            s.put("paketlendi",   count(CargoStatus.PAKETLENDI));
            s.put("dagitimda",    count(CargoStatus.DAGITIMDA));
            s.put("teslim",       count(CargoStatus.TESLIM_EDILDI));
            s.put("iptal",        count(CargoStatus.IPTAL));
            s.put("paketSayisi",  packages.size());
            s.put("avlYukseklik", avl.treeHeight());
            s.put("kuyrukBoyut",  queue.size());
            s.put("stackBoyut",   stack.size());
            return s;
        }

        private long count(CargoStatus st) {
            return cargos.values().stream().filter(c -> c.status == st).count();
        }

        // ── Yardımcı erişimciler ─────────────
        public List<Cargo>   getAllCargosSorted()   { return avl.inorder(); }
        public List<Cargo>   getWaitingQueue()      { return queue.allItems(); }
        public List<Package> getPackagesList()      { return new ArrayList<>(packages.values()); }
        public List<ActionStack.Action> getHistory(){ return stack.history(); }
        public Cargo         searchCargo(String id) { return avl.search(id); }
    }
}
