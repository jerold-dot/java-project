import os
import math
from PIL import Image, ImageDraw, ImageFont

# Set up dimensions for high-resolution PNG (2400 x 1750)
WIDTH = 2500
HEIGHT = 1800
BG_COLOR = (255, 255, 255)
LINE_COLOR = (0, 0, 0)
TEXT_COLOR = (0, 0, 0)
BORDER_WIDTH = 3

img = Image.new("RGB", (WIDTH, HEIGHT), BG_COLOR)
draw = ImageDraw.Draw(img)

# Fonts
try:
    font_title = ImageFont.truetype("arialbd.ttf", 24)
    font_bold = ImageFont.truetype("arialbd.ttf", 19)
    font_norm = ImageFont.truetype("arial.ttf", 18)
    font_italic = ImageFont.truetype("ariali.ttf", 18)
    font_caption = ImageFont.truetype("timesbd.ttf", 28)
except Exception:
    font_title = ImageFont.load_default()
    font_bold = ImageFont.load_default()
    font_norm = ImageFont.load_default()
    font_italic = ImageFont.load_default()
    font_caption = ImageFont.load_default()

def draw_class_box(x, y, w, title_lines, attributes, methods, is_interface=False, is_abstract=False):
    """Draws a standard 3-compartment UML class box."""
    # Calculate heights
    line_h = 28
    pad_y = 12
    pad_x = 14
    
    title_h = pad_y * 2 + len(title_lines) * line_h
    attr_h = pad_y * 2 + max(len(attributes), 1) * line_h if attributes is not None else 0
    meth_h = pad_y * 2 + max(len(methods), 1) * line_h if methods is not None else 0
    
    total_h = title_h + attr_h + meth_h
    
    # Outer box
    draw.rectangle([x, y, x + w, y + total_h], fill=(255, 255, 255), outline=LINE_COLOR, width=BORDER_WIDTH)
    
    # Title section
    cur_y = y + pad_y
    for line in title_lines:
        bbox = draw.textbbox((0, 0), line, font=font_title if ("<<" not in line) else font_italic)
        text_w = bbox[2] - bbox[0]
        draw.text((x + (w - text_w) / 2, cur_y), line, fill=TEXT_COLOR, font=font_title if ("<<" not in line) else font_italic)
        cur_y += line_h
        
    # Attribute section
    if attributes is not None:
        draw.line([x, y + title_h, x + w, y + title_h], fill=LINE_COLOR, width=BORDER_WIDTH)
        cur_y = y + title_h + pad_y
        for attr in attributes:
            draw.text((x + pad_x, cur_y), attr, fill=TEXT_COLOR, font=font_norm)
            cur_y += line_h
            
    # Method section
    if methods is not None:
        div_y = y + title_h + attr_h
        draw.line([x, div_y, x + w, div_y], fill=LINE_COLOR, width=BORDER_WIDTH)
        cur_y = div_y + pad_y
        for m in methods:
            draw.text((x + pad_x, cur_y), m, fill=TEXT_COLOR, font=font_norm)
            cur_y += line_h
            
    return {"x": x, "y": y, "w": w, "h": total_h, 
            "top": (x + w/2, y),
            "bottom": (x + w/2, y + total_h),
            "left": (x, y + total_h/2),
            "right": (x + w, y + total_h/2)}

def draw_inheritance_arrow(start_pt, end_pt, dashed=False):
    """Draws a UML generalization/realization arrow with a hollow triangle head."""
    sx, sy = start_pt
    ex, ey = end_pt
    
    head_size = 20
    # Determine direction
    dx = ex - sx
    dy = ey - sy
    length = math.hypot(dx, dy)
    if length == 0:
        return
    ux = dx / length
    uy = dy / length
    
    # Point where line stops (base of triangle)
    base_x = ex - ux * head_size
    base_y = ey - uy * head_size
    
    # Triangle perpendiculars
    perp_x = -uy * (head_size * 0.6)
    perp_y = ux * (head_size * 0.6)
    
    p1 = (ex, ey)
    p2 = (base_x + perp_x, base_y + perp_y)
    p3 = (base_x - perp_x, base_y - perp_y)
    
    # Draw line
    if dashed:
        # dashed line
        dash_len = 10
        gap_len = 6
        curr = 0
        total_line = math.hypot(base_x - sx, base_y - sy)
        while curr < total_line:
            seg_start = curr
            seg_end = min(curr + dash_len, total_line)
            x1 = sx + ux * seg_start
            y1 = sy + uy * seg_start
            x2 = sx + ux * seg_end
            y2 = sy + uy * seg_end
            draw.line([x1, y1, x2, y2], fill=LINE_COLOR, width=BORDER_WIDTH)
            curr += dash_len + gap_len
    else:
        draw.line([sx, sy, base_x, base_y], fill=LINE_COLOR, width=BORDER_WIDTH)
        
    # Draw hollow triangle
    draw.polygon([p1, p2, p3], fill=(255, 255, 255), outline=LINE_COLOR)
    draw.line([p1, p2], fill=LINE_COLOR, width=BORDER_WIDTH)
    draw.line([p2, p3], fill=LINE_COLOR, width=BORDER_WIDTH)
    draw.line([p3, p1], fill=LINE_COLOR, width=BORDER_WIDTH)

def draw_diamond_arrow(start_pt, end_pt, filled=True):
    """Draws composition/aggregation line with diamond."""
    sx, sy = start_pt
    ex, ey = end_pt
    d_size = 18
    dx = sx - ex
    dy = sy - ey
    l = math.hypot(dx, dy)
    if l == 0: return
    ux = dx / l
    uy = dy / l
    
    # Diamond points at start_pt
    p1 = (sx, sy)
    p2 = (sx - ux * d_size - uy * (d_size * 0.5), sy - uy * d_size + ux * (d_size * 0.5))
    p3 = (sx - ux * (d_size * 2), sy - uy * (d_size * 2))
    p4 = (sx - ux * d_size + uy * (d_size * 0.5), sy - uy * d_size - ux * (d_size * 0.5))
    
    fill_col = LINE_COLOR if filled else (255, 255, 255)
    draw.polygon([p1, p2, p3, p4], fill=fill_col, outline=LINE_COLOR)
    draw.line([p1, p2], fill=LINE_COLOR, width=BORDER_WIDTH)
    draw.line([p2, p3], fill=LINE_COLOR, width=BORDER_WIDTH)
    draw.line([p3, p4], fill=LINE_COLOR, width=BORDER_WIDTH)
    draw.line([p4, p1], fill=LINE_COLOR, width=BORDER_WIDTH)
    
    # Line from diamond tip (p3) to end_pt
    draw.line([p3[0], p3[1], ex, ey], fill=LINE_COLOR, width=BORDER_WIDTH)

# ==================== DRAW CLASSES ====================

# 1. <<interface>> Billable (Top Left)
box_billable = draw_class_box(
    x=100, y=90, w=440,
    title_lines=["<<interface>>", "Billable"],
    attributes=[],
    methods=["+calculateTotalValue(): double"]
)

# 2. <<abstract>> MedicineCategory (Top Center)
box_category = draw_class_box(
    x=680, y=80, w=480,
    title_lines=["<<abstract>>", "MedicineCategory"],
    attributes=["-categoryName: String"],
    methods=["+getCategory(): String {abstract}"]
)

# 3. DatabaseHelper (Top Right)
box_db = draw_class_box(
    x=1750, y=70, w=650,
    title_lines=["DatabaseHelper"],
    attributes=[
        "-DB_URL: String = \"jdbc:mysql://...\"",
        "-USER: String = \"root\"",
        "-PASSWORD: String"
    ],
    methods=[
        "+getConnection(): Connection",
        "+initializeDatabase(): void",
        "+saveOrUpdateMedicine(m: MedicineBatch): boolean",
        "+getAllMedicines(): List<MedicineBatch>",
        "+updateStock(id: String, newQty: int): void",
        "+saveTransaction(t: TransactionRecord): boolean",
        "+getAllTransactions(): List<TransactionRecord>",
        "+getNextBillNumber(): int"
    ]
)

# 4. MedicineBatch (Center - core entity)
box_batch = draw_class_box(
    x=460, y=420, w=540,
    title_lines=["MedicineBatch"],
    attributes=[
        "-id: String",
        "-name: String",
        "-batchNumber: String",
        "-quantity: int",
        "-manufacturingDate: LocalDate",
        "-expiryDate: LocalDate",
        "-supplier: String",
        "-unitPrice: double"
    ],
    methods=[
        "+MedicineBatch(id, name, batchNo, qty, ...)",
        "+getCategory(): String",
        "+calculateTotalValue(): double",
        "+getId(): String",
        "+getName(): String",
        "+getQuantity(): int",
        "+setQuantity(quantity: int): void",
        "+getUnitPrice(): double",
        "+setUnitPrice(unitPrice: double): void",
        "+setExpiryDate(expiryDate: LocalDate): void",
        "+setSupplier(supplier: String): void"
    ]
)

# 5. SaleItem (Middle Right)
box_sale = draw_class_box(
    x=1200, y=490, w=450,
    title_lines=["SaleItem"],
    attributes=[
        "-batch: MedicineBatch",
        "-quantitySold: int",
        "-subtotal: double"
    ],
    methods=[
        "+SaleItem(batch: MedicineBatch, qty: int)"
    ]
)

# 6. TransactionRecord (Bottom Right)
box_tx = draw_class_box(
    x=1200, y=930, w=480,
    title_lines=["TransactionRecord"],
    attributes=[
        "-billId: String",
        "-customerName: String",
        "-date: LocalDate",
        "-items: List<SaleItem>",
        "-totalAmount: double"
    ],
    methods=[
        "+TransactionRecord(billId, cust, date, items, total)"
    ]
)

# 7. PharmacyFrame (GUI - Bottom Left)
box_gui = draw_class_box(
    x=100, y=1160, w=600,
    title_lines=["PharmacyFrame", "(extends JFrame)"],
    attributes=[
        "-stockTable: JTable",
        "-cartTable: JTable",
        "-historyList: JList<TransactionRecord>",
        "-alertsArea: JTextArea"
    ],
    methods=[
        "+PharmacyFrame()",
        "+inventoryPanel(): JPanel",
        "+billingPanel(): JPanel",
        "+historyPanel(): JPanel",
        "+alertsPanel(): JPanel",
        "+stockRefillDialog(): void",
        "+showInvoiceDialog(t: TransactionRecord): void",
        "+refreshAll(): void"
    ]
)

# 8. Main / PharmacyManagementSystem (Bottom Center)
box_main = draw_class_box(
    x=820, y=1240, w=500,
    title_lines=["PharmacyManagementSystem"],
    attributes=[
        "-inventory: Map<String, MedicineBatch>",
        "-transactionHistory: List<TransactionRecord>",
        "-pendingBillItems: List<SaleItem>"
    ],
    methods=[
        "+syncFromDatabase(): void",
        "+seedSampleData(): void",
        "+main(args: String[]): void"
    ]
)


# ==================== DRAW RELATIONSHIPS ====================

# 1. MedicineBatch EXTENDS MedicineCategory (solid line with hollow triangle)
# Line from top of box_batch to bottom of box_category
draw_inheritance_arrow(
    (box_batch["x"] + 360, box_batch["y"]),
    (box_category["x"] + 240, box_category["y"] + box_category["h"]),
    dashed=False
)
draw.text((box_batch["x"] + 375, box_batch["y"] - 130), "extends", font=font_italic, fill=TEXT_COLOR)

# 2. MedicineBatch IMPLEMENTS Billable (dashed line with hollow triangle)
# Orthogonal path from left of box_batch up to bottom of box_billable
p_start = (box_batch["x"] + 80, box_batch["y"])
p_mid = (box_batch["x"] + 80, box_billable["y"] + box_billable["h"] + 80)
p_end_x = box_billable["x"] + box_billable["w"] / 2
p_end_y = box_billable["y"] + box_billable["h"]

# Draw orthogonal dashed line
draw.line([p_start[0], p_start[1], p_mid[0], p_mid[1]], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.line([p_mid[0], p_mid[1], p_end_x, p_mid[1]], fill=LINE_COLOR, width=BORDER_WIDTH)
draw_inheritance_arrow((p_end_x, p_mid[1]), (p_end_x, p_end_y), dashed=True)
draw.text((p_end_x + 15, p_mid[1] - 40), "implements", font=font_italic, fill=TEXT_COLOR)

# 3. SaleItem ASSOCIATES WITH MedicineBatch (1 batch per sale item)
draw.line([box_batch["right"][0], box_batch["right"][1] - 60, box_sale["left"][0], box_sale["left"][1]], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.text((box_batch["right"][0] + 15, box_batch["right"][1] - 95), "1", font=font_bold, fill=TEXT_COLOR)
draw.text((box_sale["left"][0] - 40, box_sale["left"][1] - 35), "0..*", font=font_bold, fill=TEXT_COLOR)
draw.text((box_batch["right"][0] + 60, box_batch["right"][1] - 85), "references >", font=font_italic, fill=TEXT_COLOR)

# 4. TransactionRecord HAS-A (COMPOSITION) SaleItem (1 transaction has 1..* items)
draw_diamond_arrow(
    (box_tx["x"] + 200, box_tx["y"]),
    (box_sale["x"] + 200, box_sale["y"] + box_sale["h"]),
    filled=True
)
draw.text((box_tx["x"] + 215, box_tx["y"] - 50), "1", font=font_bold, fill=TEXT_COLOR)
draw.text((box_sale["x"] + 215, box_sale["y"] + box_sale["h"] + 20), "1..*", font=font_bold, fill=TEXT_COLOR)
draw.text((box_tx["x"] + 225, box_tx["y"] - 110), "contains", font=font_italic, fill=TEXT_COLOR)

# 5. DatabaseHelper PERSISTS MedicineBatch and TransactionRecord (Association lines)
# Orthogonal path from DatabaseHelper to MedicineBatch
draw.line([box_db["left"][0], box_db["left"][1] + 60, 1070, box_db["left"][1] + 60], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.line([1070, box_db["left"][1] + 60, 1070, box_batch["right"][1] + 30], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.line([1070, box_batch["right"][1] + 30, box_batch["right"][0], box_batch["right"][1] + 30], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.text((1080, 370), "persists / reads >", font=font_italic, fill=TEXT_COLOR)

# DatabaseHelper to TransactionRecord
draw.line([box_db["x"] + 300, box_db["bottom"][1], box_db["x"] + 300, box_tx["right"][1]], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.line([box_db["x"] + 300, box_tx["right"][1], box_tx["right"][0], box_tx["right"][1]], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.text((box_tx["right"][0] + 20, box_tx["right"][1] - 30), "< saves / loads", font=font_italic, fill=TEXT_COLOR)

# 6. PharmacyFrame associates with PharmacyManagementSystem
draw.line([box_gui["right"][0], box_gui["right"][1], box_main["left"][0], box_main["left"][1]], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.text((box_gui["right"][0] + 20, box_gui["right"][1] - 30), "launches / views >", font=font_italic, fill=TEXT_COLOR)

# 7. PharmacyManagementSystem manages MedicineBatch & TransactionRecord
draw.line([box_main["x"] + 250, box_main["y"], box_main["x"] + 250, 1150], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.line([box_main["x"] + 250, 1150, box_batch["bottom"][0], 1150], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.line([box_batch["bottom"][0], 1150, box_batch["bottom"][0], box_batch["bottom"][1]], fill=LINE_COLOR, width=BORDER_WIDTH)
draw.text((box_batch["bottom"][0] + 15, 1120), "manages stock >", font=font_italic, fill=TEXT_COLOR)


# ==================== CAPTION (Matching the sample image) ====================
caption_text = "Figure 5.1: Class Diagram of Pharmacy Management System"
bbox = draw.textbbox((0, 0), caption_text, font=font_caption)
cap_w = bbox[2] - bbox[0]
draw.text(((WIDTH - cap_w) / 2, HEIGHT - 70), caption_text, font=font_caption, fill=TEXT_COLOR)

# Save high-res PNG
png_path = "class_diagram.png"
img.save(png_path, "PNG", dpi=(300, 300))
print(f"Successfully generated {png_path} ({WIDTH}x{HEIGHT})")
