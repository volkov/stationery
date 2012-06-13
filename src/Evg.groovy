
class Evg {
  
  def alpha = 0.3f
  
  def demand(item,store,day,db){
    return db.content.findOne(item:item._id,store:store._id,day:day-365).demand
  }
  
  def sstat(i,s,t,db){
    return db.content.findOne(item:i._id,store:s._id,day:t).demand
  }
  def sproj(i,s,day,T,db){
    def sum=0;
    def n=0;;
    def f;
    db.content.find(item:i._id,store:s._id).sort(day:1).each {
      sum+=it.demand
      n++
      if ((day-it.day)%T==0) {
        if (n==T){
          if (f==null){
            f=sum
          } else {
            f=alpha*sum+(1-alpha)*f
          }
        }
        n=0
        sum=0
      }
    }
    return f
  }

  def run(db,day){
    println "------ day ${day} ------"
    def stores = wrap(db.store.find(type:"store"))
    def items = wrap(db.item.find())
    items.each{println it}

    stores.each { s ->
      println "----- store:${s.name} -----"
      def pu = [:]
      println "--- pu dump ---"
      def sprojd=[:]
      items.each { i ->
        def savgp = (day-364..day-364+7).inject(0) {a,b -> a + sstat(i,s,b,db)} / 7.0
        def savg = (day-6..day).inject(0) {a,b -> a + sstat(i,s,b,db)}/7.0
        def di = ((day-6..day).inject(0) {a,b -> a + (savg-sstat(i,s,b,db)) ** 2}/7.0) ** 0.5
        sprojd[i] = sproj(i,s,day,1,db)
        def sstatd = sstat(i,s,day,db)
        def R = di * Math.signum(sprojd[i]-sstatd);
        pu[i] = i.nu*(alpha*savgp+(1-alpha)*sprojd[i]+R)
        db.lorder<<[store:s._id,item:i._id,day:day,lorder:pu[i]]
        println "${i.name},day:${day},savgp:${savgp},savg:${savg},sproj:${sprojd[i]},sstat:${sstatd},R:${R},di:${di},pu[i]:${pu[i]}"
      }
      def spuvi = items.inject(0) {a,item -> a+pu[item]*item.v}
      println "--- spuvi:${spuvi}"
      def nmax = [:]
      println "--- nmax dump ---"
      items.each{ i ->
        nmax[i]=s.v/(spuvi*i.v)*pu[i]
        println "${i.name} nmax:${nmax[i]}"
      }
      def t = getT(db,s)
      println "--- t:${t} ---"
      def sstar = [:]
      println "--- sstar dump---"
      def itemsk = []
      def n = [:]
      items.each { i ->
        n[i] = db.content.findOne(store:s._id,item:i._id,day:day).n
        def sum = 0
        if (n[i]-sprojd[i]<=pu[i]){
          sstar[i]=sproj(i,s,day,t,db)
          itemsk<<i
          println "${i.name} sstar:${sstar[i]}"
        }
      }
      if (itemsk.isEmpty()) return;
      def qdiv = 0;
      itemsk.each {
        qdiv+=sstar[it]*it.ro*it.price*t
      }
      println "--- qdiv:${qdiv}---"
      def qm = (2*s.k*365/qdiv)**0.5
      println "--- qm:${qm}---"
      println "--- qstar dump---"
      def qstar = [:]
      def qq = [:]
      itemsk.each {
        qstar[it]=sstar[it]*qm
        if (1/qm<t) {
          qq[it] = [qstar[it], nmax[it]-n[it]].min()
        } else {
          qq[it] = [
            demand(it,s,day+1),
            nmax[it]-n[it]
          ].min()
        }
        println "${it.name} qstar:${qstar[it]} qq:${qq[it]}"
      }
      def qsum = 0;
      itemsk.each {
        qsum+=qq[it]*it.v
      }
      println "---- qsum:${qsum}"

      if (qsum>1){
        def deliveryItems = []
        itemsk.each {
          deliveryItems << [item:it._id,n:(Integer)qq[it].round()]
        }
        println deliveryItems
        def warehouse = db.store.findOne(type:"warehouse")
        db.delivery << [from:warehouse._id,to:s._id,items:deliveryItems,day:day]
      }
    }
  }

  def getT(db,store) {
    /*def deliveryDates = []
    db.deliveries.find(store:store._id).sort(day:-1).limit(2).each{ deliveryDates<<it.day}
    if (deliveryDates.size()>1){
      return deliveryDates[1]-deliveryDates[0]
    }*/
    return 30
  }

  def wrap(cursor){
    def result = []
    cursor.each {
      def item = [:]
      it.each{k,v ->
        item[k]=v
      }
      result << item
    }
    return result
  }
}
