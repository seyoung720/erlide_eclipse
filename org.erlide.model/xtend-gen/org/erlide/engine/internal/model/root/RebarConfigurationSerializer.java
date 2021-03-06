package org.erlide.engine.internal.model.root;

import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangString;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.erlide.engine.ErlangEngine;
import org.erlide.engine.IErlangEngine;
import org.erlide.engine.model.root.ErlangProjectProperties;
import org.erlide.engine.model.root.ProjectConfigurationSerializer;
import org.erlide.engine.services.parsing.SimpleParserService;
import org.erlide.util.erlang.OtpBindings;
import org.erlide.util.erlang.OtpErlang;

@SuppressWarnings("all")
public class RebarConfigurationSerializer implements ProjectConfigurationSerializer {
  @Override
  public String encodeConfig(final ErlangProjectProperties info) {
    return null;
  }
  
  @Override
  public ErlangProjectProperties decodeConfig(final String config) {
    ErlangProjectProperties _xblockexpression = null;
    {
      final ErlangProjectProperties result = new ErlangProjectProperties();
      Path _path = new Path("ebin");
      result.setOutputDir(_path);
      IErlangEngine _instance = ErlangEngine.getInstance();
      SimpleParserService _simpleParserService = _instance.getSimpleParserService();
      final List<OtpErlangObject> content = _simpleParserService.parse(config);
      boolean _isEmpty = content.isEmpty();
      if (_isEmpty) {
        return result;
      }
      final Procedure1<OtpErlangObject> _function = new Procedure1<OtpErlangObject>() {
        @Override
        public void apply(final OtpErlangObject erl_opts) {
          try {
            final OtpBindings bindings = OtpErlang.match("{erl_opts,Opts}", erl_opts);
            if ((bindings != null)) {
              final Collection<OtpErlangObject> opts = bindings.getList("Opts");
              if ((opts != null)) {
                final Procedure1<OtpErlangObject> _function = new Procedure1<OtpErlangObject>() {
                  @Override
                  public void apply(final OtpErlangObject opt) {
                    try {
                      final OtpBindings b = OtpErlang.match("{Tag,Arg}", opt);
                      if ((b != null)) {
                        RebarConfigurationSerializer.this.parseOption(result, b);
                      }
                    } catch (Throwable _e) {
                      throw Exceptions.sneakyThrow(_e);
                    }
                  }
                };
                IterableExtensions.<OtpErlangObject>forEach(opts, _function);
              }
            }
          } catch (Throwable _e) {
            throw Exceptions.sneakyThrow(_e);
          }
        }
      };
      IterableExtensions.<OtpErlangObject>forEach(content, _function);
      _xblockexpression = result;
    }
    return _xblockexpression;
  }
  
  public void parseOption(final ErlangProjectProperties result, final OtpBindings b) {
    try {
      String _atom = b.getAtom("Tag");
      switch (_atom) {
        case "i":
          String _string = b.getString("Arg");
          final Path inc = new Path(_string);
          Collection<IPath> _includeDirs = result.getIncludeDirs();
          boolean _contains = _includeDirs.contains(inc);
          boolean _not = (!_contains);
          if (_not) {
            Collection<IPath> _includeDirs_1 = result.getIncludeDirs();
            final List<IPath> incs = CollectionLiterals.<IPath>newArrayList(((IPath[])Conversions.unwrapArray(_includeDirs_1, IPath.class)));
            incs.add(inc);
            result.setIncludeDirs(incs);
          }
          break;
        case "src_dirs":
          Collection<OtpErlangObject> _list = b.getList("Arg");
          final Function1<OtpErlangObject, Path> _function = new Function1<OtpErlangObject, Path>() {
            @Override
            public Path apply(final OtpErlangObject it) {
              Path _xblockexpression = null;
              {
                String _xifexpression = null;
                if ((it instanceof OtpErlangString)) {
                  _xifexpression = ((OtpErlangString)it).stringValue();
                } else {
                  _xifexpression = it.toString();
                }
                final String s = _xifexpression;
                _xblockexpression = new Path(s);
              }
              return _xblockexpression;
            }
          };
          Iterable<Path> _map = IterableExtensions.<OtpErlangObject, Path>map(_list, _function);
          result.setSourceDirs(((IPath[])Conversions.unwrapArray(_map, IPath.class)));
          break;
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
