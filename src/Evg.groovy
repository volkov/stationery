
class Evg {

  def demand(item,store,day){
    if (store.type != 'warehouse') return item.demand
    return 0
  }

  def run(db,day){
    def pu = [:]
    def stores = {return db.store.find(type:"store")}
    def items = {return db.item.find()}
    items().each{println it}
    
    println "--- pu dump ---"
    stores().each { s ->
      items().each { i ->
        def savgp = (day-365..day-365+6).inject(0) {a,b -> a + demand(i,s,b)} / 7.0
        def savgi = (day+1..day+7).inject(0) {a,b -> a + demand(i,s,b)}/7.0
        def savg = (day-6..day).inject(0) {a,b -> a + demand(i,s,b)}/7.0
        def di = ((day-6..day).inject(0) {a,b -> (a + savg-demand(i,s,b)) ** 2}/7.0) ** 0.5
        pu[i] = [savgp,savgi].max()+di
        println "store:${s.name},item:${i.name},demand:${i.demand},day:${day},savgp:${savgp},savgi:${savgi},savg:${savg},di:${di},pu[i]:${pu[i]}"
      }
    }
    def spuvi = items().inject(0) {s,item -> s+pu[item]*item.v}
    println "--- spuvi:${spuvi}"
    def nmax = [:]
    println "--- nmax dump ---"
    stores().each { s ->
        println "store:${s.name}"
        items().each{ i ->
          nmax[[s,i]]=s.v/spuvi*i.v*pu[i]
          println "${i.name} nmax:${nmax[[s,i]]}"
        }
    }
  }
}
