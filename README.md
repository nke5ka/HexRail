# HexRail

Welcome all, to HEX RAIL!  This program creates random terrain, random cities upon terrain, and paths between cities upon the terrain.


## Pictures of HexRail in action

### LET THERE BE LIGHT
Select your size, etc. options and through the magic of the `diamond square` algorithm, a world of wonder shall appear before you.
![world gen](/img/terrain.png)

### Get Connected!
Through the magic of `A*`, a path is found through the world, following these general ideas:
* Steep railways and roads are hard to use so we should avoid them even if a longer path is needed.
* Bridges are expensive so water should be avoided.
* Shorter is better, barring obstacles.
![connect 2 cities](/img/route.png)

### Visit them all!
Connect all the cities using the least length of railways using `Kruskal's Algorithm` to form a minimum spanning forest.
![connect all cities](/img/MST.png)

### Choo choo
Coming soon!

## DIY! How do I run it?
If you trust an executable from a random stranger on the internet, feel free to use the [jar file](./runHexRail.jar)

Alternatively you can use Eclipse to open this project, and click the play button from `src/hexRail/AAAHexLaunch.java`

Finally, you can opt to run the following command line commands in your git bash terminal if you use windows - I have not extensively tested this as my IDEs are Eclipse and VisualStudioCode
* `cd ./HexRail_Legacy`
* `javac src/hexRail/*.java -d ./bin`
* `cd bin`
* `java hexRail.AAAHexLaunch`

Fun Fact: This program has been slowly added to, removed from, rewritten, and recombined since my high school years.  I even keep pictures of it in my wallet next to pictures of my significant other.  It is truly a labor of love.  The next reincarnation will be in javascript, for easy consumption from any browser.