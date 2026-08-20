from flask import Flask, render_template, request, redirect, url_for

app = Flask(__name__)

products = [
    {"id": 1, "name": "Laptop Pro", "description": "High-performance laptop for professionals", "price": 999.99, "image": "https://via.placeholder.com/150?text=Laptop"},
    {"id": 2, "name": "Smartphone X", "description": "Latest flagship smartphone", "price": 699.99, "image": "https://via.placeholder.com/150?text=Phone"},
    {"id": 3, "name": "Wireless Headphones", "description": "Premium noise-cancelling headphones", "price": 299.99, "image": "https://via.placeholder.com/150?text=Headphones"},
    {"id": 4, "name": "Tablet Ultra", "description": "Lightweight tablet with stunning display", "price": 499.99, "image": "https://via.placeholder.com/150?text=Tablet"},
]

@app.route("/")
def index():
    return render_template("index.html", products=products)

@app.route("/product/<int:product_id>")
def product_detail(product_id):
    product = None
    for p in products:
        if p["id"] == product_id:
            product = p
    return render_template("product.html", product=product)

@app.route("/add", methods=["GET", "POST"])
def add_product():
    if request.method == "POST":
        new_product = {
            "id": len(products) + 1,
            "name": request.form["name"],
            "description": request.form["description"],
            "price": float(request.form["price"]),
            "image": request.form.get("image", "https://via.placeholder.com/150?text=Product"),
        }
        products.append(new_product)
        return redirect(url_for("index"))
    return render_template("add_product.html")

@app.route("/edit/<int:product_id>", methods=["GET", "POST"])
def edit_product(product_id):
    product = None
    for p in products:
        if p["id"] == product_id:
            product = p
    if request.method == "POST" and product is not None:
        product["name"] = request.form["name"]
        product["description"] = request.form["description"]
        product["price"] = float(request.form["price"])
        product["image"] = request.form.get("image", product["image"])
        return redirect(url_for("index"))
    return render_template("edit_product.html", product=product)

@app.route("/delete/<int:product_id>")
def delete_product(product_id):
    global products
    products = [p for p in products if p["id"] != product_id]
    return redirect(url_for("index"))

if __name__ == "__main__":
    app.run(debug=True)
