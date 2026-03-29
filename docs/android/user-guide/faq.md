# Frequently Asked Questions — Android

---

### Account & Login

**Q: I forgot my password. How do I reset it?**
On the login screen tap **Forgot Password**, enter your email, and tap **Send Reset Link**. You will receive an email from Firebase with a link to set a new password. Check your spam folder if it does not arrive within a minute.

**Q: Can I use the app without an account?**
No — an account is required. This ensures your data is tied to your identity. Google sign-in is the quickest way to get started.

**Q: Can I sign in on multiple devices?**
You can sign in on another device with the same account, but your data is stored locally on each device separately. Data does not sync between devices yet (cloud sync is on the roadmap).

**Q: How do I delete my account?**
Go to **Profile → Delete Account**. This permanently deletes all your data from the device and removes your account from Firebase. There is no recovery — make sure you want to do this.

---

### Data & Privacy

**Q: Where is my data stored?**
All meal, diet, log, and health data is stored **on your device only** in a local SQLite database. Your credentials are managed by Firebase Authentication. No meal or health data is sent to any server.

**Q: What happens if I uninstall the app?**
Your local data is deleted. We recommend exporting your data (Settings → Export) before uninstalling.

**Q: Can I export my data?**
Yes. Go to **Settings → Export Data** to download a CSV of your food database, meals, and health metrics.

---

### Foods & Nutrition

**Q: The barcode scan did not find my product. What do I do?**
The app queries OpenFoodFacts and USDA databases. If the product is not in either, tap **Add Manually** to enter the nutrition data from the product label yourself.

**Q: Nutritional data is per 100g — how does the app handle different serving sizes?**
When you add a food to a meal, you enter the quantity and unit (e.g. 150g, 1 cup). The app scales the nutrition data accordingly. For example, 150g of a food with 200 kcal/100g = 300 kcal.

**Q: Can I edit a food I already added?**
Yes. Open the food in the Foods screen and tap the edit icon. Changes apply retroactively to any meals using that food.

---

### Meals & Diets

**Q: What is the difference between a Meal and a Diet?**
A **Meal** is one eating occasion (e.g. "Oats with Milk"). A **Diet** is a full day's template that groups multiple meals into slots (Breakfast, Lunch, Dinner, etc.).

**Q: Can I assign the same diet to multiple days?**
Yes. Go to the Calendar, tap each day, and assign the same diet. The diet template is reused — editing the diet updates it for future uses, but does not retroactively change logged history.

**Q: What are Tags?**
Tags are coloured labels you attach to diets to categorise them (e.g. "Remission", "High Protein"). They are for your personal organisation and do not affect nutrition calculations.

---

### Logging

**Q: I applied the wrong diet to a day. How do I fix it?**
Go to the daily log for that day → tap the diet chip at the top → choose **Change** or **Clear**. Logged foods are not affected — only the planned diet assignment changes.

**Q: Can I log the same food multiple times in a day?**
Yes. You can add a food to multiple slots or add it multiple times to the same slot with different quantities.

---

### Widgets

**Q: The widget is not updating.**
Open the app once — the widget observes the database and should update automatically. If it persists, try removing and re-adding the widget.

**Q: Why is the widget blank?**
No diet is assigned to today. Go to the Meal Plan tab and assign a diet to today.

---

### Health Metrics

**Q: Can I track custom health metrics?**
Yes. Go to **Health → Manage Types → +** to create any metric with a custom name and unit.

**Q: Can I delete an incorrect metric reading?**
Yes. Tap the entry in the Health list → tap **Delete**.
