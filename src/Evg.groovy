
class Evg {

  def demand(item,store,day){
    if (store.type != 'warehouse') return item.demand
    return 0
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
      items.each { i ->
        def savgp = (day-365..day-365+6).inject(0) {a,b -> a + demand(i,s,b)} / 7.0
        def savgi = (day+1..day+7).inject(0) {a,b -> a + demand(i,s,b)}/7.0
        def savg = (day-6..day).inject(0) {a,b -> a + demand(i,s,b)}/7.0
        def di = ((day-365..day-365+6).inject(0) {a,b -> (a + savg-demand(i,s,b)) ** 2}/7.0) ** 0.5
        pu[i] = [savgp, savgi].max()+di
        println "store:${s.name},item:${i.name},demand:${i.demand},day:${day},savgp:${savgp},savgi:${savgi},savg:${savg},di:${di},pu[i]:${pu[i]}"
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
        if (n[i]-demand(i,s,day+1)<=pu[i]){
          sstar[i]=(1..t).inject(0) {a,b -> a+demand(i,s,day+b)}
          itemsk<<i
          println "${i.name} sstar:${sstar[i]}"
        }
      }
      if (itemsk.isEmpty()) return;
      def qdiv = 0;
      itemsk.each {
        qdiv+=sstar[it]*it.ro*it.price*t/365
      }
      println "--- qdiv:${qdiv}---"
      def qm = (s.k/qdiv)**0.5
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
    def deliveryDates = []
    db.deliveries.find(store:store._id).sort(day:-1).limit(2).each{ deliveryDates<<it.day}
    if (deliveryDates.size()>1){
      return deliveryDates[1]-deliveryDates[0]
    }
    return 7
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
