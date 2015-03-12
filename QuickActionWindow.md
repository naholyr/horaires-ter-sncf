

# Installation #

  1. [Download JAR](http://code.google.com/p/horaires-ter-sncf/downloads/detail?name=QuickActionWindow-1.0.jar) (it contains compiled class AND source code so you can explore it if you want).
  1. Copy the JAR file to the "lib/" folder of your project.
  1. Right-click on the JAR file and choose "Build Path" then "Add to Build Path"

That's all ! you can use Quick Action windows in your project :)

# Usage #

## Sample project ##

Look at the sample project for a full-featured example.
  * You can [directly download it](http://code.google.com/p/horaires-ter-sncf/downloads/detail?name=QuickActionWindowSample.zip) as a ZIP archive
  * Or you can checkout the SVN repository : `http://horaires-ter-sncf.googlecode.com/svn/QuickActionWindowSample`

![http://horaires-ter-sncf.googlecode.com/files/device.png](http://horaires-ter-sncf.googlecode.com/files/device.png)

## 1. General configuration ##

Define a `SparseIntArray` that will hold the configuration of your Quick Action Window.

You will use keys defined in `QuickActionWindow.Config` to store configuration options :
```
SparseIntArray configuration = new SparseIntArray();
// Mandatory options
configuration.put(QuickActionWindow.Config.WINDOW_LAYOUT, R.layout.quick_action_window);
configuration.put(QuickActionWindow.Config.ITEM_LAYOUT, R.layout.quick_action_item);
// Other options : see documentation of QuickActionWindow.Config...
// ...

QuickActionWindow window = QuickActionWindow.getWindow(context, configuration);
```

### Available options ###

  * `WINDOW_LAYOUT` : Window layout ID.
  * `ITEM_LAYOUT` : Quick action item layout ID.
  * `WINDOW_BACKGROUND_IF_ABOVE` : Background applied to window content view (root view of the window layout) when window is above the anchor. Leave null if your default background is for above position.
  * `WINDOW_BACKGROUND_IF_BELOW` : Background applied to window content view (root view of the window layout) when window is below the anchor. Leave null if your default background is for below position.
  * `WINDOW_ANIMATION_STYLE` : Quick action window animation style (for opening and closing animations). Leave null to disable.
  * `ITEM_APPEAR_ANIMATION` : Animation played on each item layout when window is open. Leave null to disable.
  * `CONTAINER` : Subview of the content view which will hold all items. Leave null to use the root view directly.
  * `ITEM_ICON` : ImageView for the item icon. Leave null if your items won't show icons.
  * `ITEM_LABEL` : TextView for the item label. Leave null if your items won't show label.
  * `ARROW_OFFSET` : Vertical offset applied to the window, so that the arrow will overlap a little the anchor. Best value should be between 50% and 75% of the height of the arrow in your window background.

### Prepare your layout ###

As you will see in the QuickActionWindowSample project, and corresponding to the available options listed above, there are a few elements to prepare :
  * The XML layout for your quick action window. It can be either a ViewGroup itself and items will be directly added to this, or define a ViewGroup that's supposed to contain items. In this last case, you must put the container's ID in configuration array under the `CONTAINER` option.
  * The XML layout for any item in the quick action window. It should contain at least a `TextView` (its ID is defined in the `ITEM_LABEL` option) and/or an `ImageView` (its ID is defined in the `ITEM_ICON` option).
  * The window layout must have a background that will define the general layout of your window. Use 9-patch for this, and as usual, take a look at the sample project...
  * You could define a few animations to add some life to this.

## 2. Adding items ##

There are two ways of adding items :
  * manually, defining an icon, a label, and a callback (which can be declared as a full callback, or just an intent to launch an activity on click).
  * dynamically, defining an intent, and the system will simply add all items (icon + label + activity on click) that can respond to this intent.

### Manually, with callback ###

```
// Declare full item
QuickActionWindow.Item item = new QuickActionWindow.Item(context, R.string.item_label, R.drawable.item_icon, new QuickActionWindow.Item.Callback() {
  public void onClick(Item item, View anchor) {
    // Describe here what happens when user clicks on this item,
    // when window is shown attached to specified anchor.
  }
});
// When window is shown, if user clicks on this item, callback will be executed
window.addItem(item);
```

### Manually, as an activity item ###

```
Intent intent= new Intent(/* intent description */);
QuickActionWindow.IntentItem item = new QuickActionWindow.IntentItem(context, R.string.item_label, R.drawable.item_icon, intent);
// When window is shown, if user clicks on this item, an activity responding to this intent will be started
window.addItem(item);
```

Note that `startActivity()` can fail and throw `ActivityNotFoundException` if no activity is found responding to this intent. You can catch this case by adding an `ErrorCallback` :
```
// ...
QuickActionWindow.IntentItem item = new QuickActionWindow.IntentItem(context, R.string.item_label, R.drawable.item_icon, intent, new QuickActionWindow.IntentItem.ErrorCallback() {
  public void onError(ActivityNotFoundException e, IntentItem item) {
    // Show error message
  }
});
// ...
```

### Dynamically, by intent ###

```
Intent intent = new Intent(/* intent description */);
// For each activity responding to given itent, we'll add an item with activity's icon
// and label. As a callback, clicking on item will launch found activity.
window.addItemsForIntent(context, intent, errorCallback);
```

Obviously, this system could be very useful for implementing plugins in your applications : declare a specific actions, and allow any activity filtering this action to be shown in the quick action window. This way, you can add your own plugins, and allow third-party plugins very easily !

This is nice, but for advertisement purpose you would like to put "fake" items that will launch to - for example - Android Market when the expected activity has not been found. And if it's found, we just remove the advertisement as the item will be here. You can do it using an additionnal array of ItemAdvertisement instances !

```
QuickActionWindow.Advertisement[] ads = new QuickActionWindow.Advertisement[] {
  new QuickActionWindow.MarketAdvertisement("my.package", ".Activity", 
    getString(R.string.advertisement), getResources().getDrawable(R.drawable.advertisement), 
    "Android Market not found"
  ), 
};
// If activity "my.pkg.Activity" is not found in the IntentItems added to the window,
// we'll add an item that will open Android Market to the detail page of "my.pkg"
// when user clicks on item.
window.addItemsForIntent(context, intent, errorCallback, advertisements);
```

### Caching items ###

`IntentItem`s are a nice feature, and the `addItemsForIntent` is even nicer. Anyway, querying system for activities could slow your application if it's done each time the window is called. To allow items to be initialized only once, you could :
  1. initialize items first time.
  1. save `window.getItems()` in a private attribute.
  1. on later calls, just call `window.setItems(savedItems)`.

All this is done for you if you use an `Initializer` when creating window :
```
int windowID = WINDOW_UNIQUE_ID;
QuickActionWindow window = QuickActionWindow.getWindow(context, configuration, new QuickActionWindow.Initializer() {
  public void setItems(QuickActionWindow window) {
    // Define window's items here
  }
}, windowID);
```

When you specify intent items this way, you could have problems using context-dependant variables, like the clicked view's position or some other elements.

There is no solution if those variables are part of the intent's description (like its `Uri`). In this case, you cannot use the initializer feature. Anyway it wouldn't help you at all as obviously you HAVE to re-query system for activities responding to the intent.

In any other situation, the contextual data will be passed as extra, and you will be able to delay these affectations using `dispatchIntentExtras`, here is a full example in a `ListActivity` :
```
final Context context = this;
getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

  // When user clicks on an item of the list
  public void onItemClick(AdapterView<?> adapter, final View anchor, final int position, final long id) {

    // Define intent applied to this item
    final Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setType("mime/type");

    // Initialize window, items will be generated only once, and later clicks on other
    // list items will not execute this portion of the code.
    QuickActionWindow window = QuickActionWindow.getWindow(context, configuration, new QuickActionWindow.Initializer() {
      public void setItems(QuickActionWindow window) {
        window.addItemsForIntent(context, intent, null);
      }
    }, windowID);

    // Add ID of the clicked item as extra
    Bundle extras = new Bundle();
    extras.put("_id", id);
    window.dispatchIntentExtras(extras, intent);

    // Show window attached to the clicked item
    window.show(anchor);
  }

});
```

Here is the same code with no items-caching :
```
final Context context = this;
getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

  // When user clicks on an item of the list
  public void onItemClick(AdapterView<?> adapter, final View anchor, final int position, final long id) {

    // Initialize window
    QuickActionWindow window = QuickActionWindow.getWindow(context, configuration)

    // Intent for the clicked item
    Intent intent = new Intent(Intent.ACTION_VIEW);
    intent.setType("mime/type");
    intent.putExtra("_id", id);

    // Add window's items
    window.addItemsForIntent(context, intent, null);

    // Show window attached to the clicked item
    window.show(anchor);
  }

});
```

Use one way or another, it depends on your tastes, your will for optimising little things (this is always arguable as it here has a clear impact on readability). Note that the best optimization we could do would be not to inflate layout each time, but there are a lot of issues when re-using such views when you have to show/dismiss your window twice or more. I'm working on it ;)

## 3. Showing window ##

Just call `show(View anchor)` and the window will appear attached to specified view. Below or above, depending on remaining space up or under the view.