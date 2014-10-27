package hex;

import water.*;
import water.api.DocGen;
import water.fvec.createInteractions;
import water.fvec.Frame;
import water.util.Log;
import water.util.RString;


/**
 * Create new factors that represent interactions of the given factors
 */
public class Interaction extends Request2 {
  static final int API_WEAVER=1; // This file has auto-gen'd doc & json fields
  static public DocGen.FieldDoc[] DOC_FIELDS; // Initialized from Auto-Gen code.

  @API(help = "Input data frame", required = true, filter = Default.class, json=true)
  public Frame source;

  @API(help = "Output data frame, containing the interaction vector", required = false, filter = Default.class, json=true)
  public String target;

  @API(help = "Column indices (0-based) of factors for which interaction is to be computed", filter=colsNamesIdxFilter.class, displayName="Interaction columns")
  public int[] factors = new int[0];
  class colsNamesIdxFilter extends MultiVecSelect { public colsNamesIdxFilter() {super("source", MultiVecSelectType.NAMES_THEN_INDEXES); } }

  @API(help = "Max. number of factor levels in pair-wise interaction terms (if enforced, one extra catch-all factor will be made)", required = true, filter = Default.class, lmin = 1, lmax = Integer.MAX_VALUE, json=true)
  public int max_factors = 100;

  @API(help = "Min. occurrence threshold for factor levels in pair-wise interaction terms", required = true, filter = Default.class, lmin = 1, lmax = Integer.MAX_VALUE, json=true)
  public int min_occurrence = 1;

  long _time;

  @Override public Response serve() {
    try {
//      if (max_factors < 1) throw new IllegalArgumentException("max_factors must be >1.");
      if (factors.length == 0) throw new IllegalArgumentException("factors must be non-empty.");
      for (int v: factors) {
        if (!source.vecs()[v].isEnum()) {
          throw new IllegalArgumentException("Column " + source.names()[v] + " is not a factor.");
        }
      }
      if (target == null) {
        target = source._key.toString() + ".interaction.";
        target += "C" + factors[0];
        for (int i=1; i<factors.length; ++i) {
          target += "_C" + factors[i];
        }
      }

      Timer time = new Timer();
      final createInteractions in = new createInteractions(this);
      H2O.submitTask(in);
      in.join();
      _time = time.time();
      Log.info(report());
      return Response.done(this);
    } catch( Throwable t ) {
      return Response.error(t);
    }
  }

  @Override public boolean toHTML( StringBuilder sb ) {
    Frame fr = UKV.get(Key.make(target));
    if (fr==null) {
      return false;
    }
    RString aft = new RString("<a href='Inspect2.html?src_key=%$key'>%key</a>");
    aft.replace("key", target);
    DocGen.HTML.section(sb, report() + "<br/>Frame '" + aft.toString() + "' contains the interaction feature.");
    return true;
  }

  private String report() {
    Frame res = UKV.get(Key.make(target));
    return "Created interaction feature " + res.names()[0]
            + " (order: " + factors.length + ") with " + res.lastVec().domain().length + " factor levels"
            + " in" + PrettyPrint.msecs(_time, true);
  }

}
