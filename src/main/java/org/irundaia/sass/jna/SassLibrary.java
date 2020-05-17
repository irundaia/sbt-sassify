/*
 * Copyright 2018 Han van Venrooij
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.irundaia.sass.jna;

import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

import java.nio.ByteBuffer;

public interface SassLibrary extends Library {
	interface Sass_Output_Style {
		int SASS_STYLE_NESTED = 0;
		int SASS_STYLE_EXPANDED = 1;
		int SASS_STYLE_COMPACT = 2;
		int SASS_STYLE_COMPRESSED = 3;
		int SASS_STYLE_INSPECT = 4;
		int SASS_STYLE_TO_SASS = 5;
	}

	interface Sass_Tag {
		int SASS_BOOLEAN = 0;
		int SASS_NUMBER = 1;
		int SASS_COLOR = 2;
		int SASS_STRING = 3;
		int SASS_LIST = 4;
		int SASS_MAP = 5;
		int SASS_NULL = 6;
		int SASS_ERROR = 7;
		int SASS_WARNING = 8;
	}

	interface Sass_Separator {
		int SASS_COMMA = 0;
		int SASS_SPACE = 1;
		int SASS_HASH = 2;
	}

	interface Sass_OP {
		int AND = 0;
		int OR = 1;
		int EQ = 2;
		int NEQ = 3;
		int GT = 4;
		int GTE = 5;
		int LT = 6;
		int LTE = 7;
		int ADD = 8;
		int SUB = 9;
		int MUL = 10;
		int DIV = 11;
		int MOD = 12;
		int NUM_OPS = 13;
	}

	interface Sass_Compiler_State {
		int SASS_COMPILER_CREATED = 0;
		int SASS_COMPILER_PARSED = 1;
		int SASS_COMPILER_EXECUTED = 2;
	}

	interface Sass_Importer_Fn extends Callback {
		SassLibrary.Sass_Import_List apply(Pointer url, Pointer cb, SassLibrary.Sass_Compiler compiler);
	}

	interface Sass_Function_Fn extends Callback {
		SassLibrary.Sass_Value apply(SassLibrary.Sass_Value Sass_ValuePtr1, Pointer cb, SassLibrary.Sass_Compiler compiler);
	}

	String libsass_version();
	String libsass_language_version();
	SassLibrary.Sass_Value sass_make_null();
	SassLibrary.Sass_Value sass_make_boolean(boolean val);
	SassLibrary.Sass_Value sass_make_string(String val);
	SassLibrary.Sass_Value sass_make_qstring(String val);
	SassLibrary.Sass_Value sass_make_number(double val, String unit);
	SassLibrary.Sass_Value sass_make_color(double r, double g, double b, double a);
	SassLibrary.Sass_Value sass_make_list(SizeT len, int sep);
	SassLibrary.Sass_Value sass_make_map(SizeT len);
	SassLibrary.Sass_Value sass_make_error(String msg);
	SassLibrary.Sass_Value sass_make_warning(String msg);
	SassLibrary.Sass_Value sass_value_op(int op, SassLibrary.Sass_Value a, SassLibrary.Sass_Value b);
	SassLibrary.Sass_Value sass_value_stringify(SassLibrary.Sass_Value a, byte compressed, int precision);
	int sass_value_get_tag(SassLibrary.Sass_Value v);
	boolean sass_value_is_null(SassLibrary.Sass_Value v);
	boolean sass_value_is_number(SassLibrary.Sass_Value v);
	boolean sass_value_is_string(SassLibrary.Sass_Value v);
	boolean sass_value_is_boolean(SassLibrary.Sass_Value v);
	boolean sass_value_is_color(SassLibrary.Sass_Value v);
	boolean sass_value_is_list(SassLibrary.Sass_Value v);
	boolean sass_value_is_map(SassLibrary.Sass_Value v);
	boolean sass_value_is_error(SassLibrary.Sass_Value v);
	boolean sass_value_is_warning(SassLibrary.Sass_Value v);
	double sass_number_get_value(SassLibrary.Sass_Value v);
	void sass_number_set_value(SassLibrary.Sass_Value v, double value);
	String sass_number_get_unit(SassLibrary.Sass_Value v);
	String sass_string_get_value(SassLibrary.Sass_Value v);
	boolean sass_boolean_get_value(SassLibrary.Sass_Value v);
	double sass_color_get_r(SassLibrary.Sass_Value v);
	double sass_color_get_g(SassLibrary.Sass_Value v);
	double sass_color_get_b(SassLibrary.Sass_Value v);
	double sass_color_get_a(SassLibrary.Sass_Value v);
	SizeT sass_list_get_length(SassLibrary.Sass_Value v);
	int sass_list_get_separator(SassLibrary.Sass_Value v);
	SassLibrary.Sass_Value sass_list_get_value(SassLibrary.Sass_Value v, SizeT i);
	void sass_list_set_value(SassLibrary.Sass_Value v, SizeT i, SassLibrary.Sass_Value value);
	SizeT sass_map_get_length(SassLibrary.Sass_Value v);
	SassLibrary.Sass_Value sass_map_get_key(SassLibrary.Sass_Value v, SizeT i);
	void sass_map_set_key(SassLibrary.Sass_Value v, SizeT i, SassLibrary.Sass_Value Sass_ValuePtr1);
	SassLibrary.Sass_Value sass_map_get_value(SassLibrary.Sass_Value v, SizeT i);
	void sass_map_set_value(SassLibrary.Sass_Value v, SizeT i, SassLibrary.Sass_Value Sass_ValuePtr1);
	Pointer sass_error_get_message(SassLibrary.Sass_Value v);
	Pointer sass_warning_get_message(SassLibrary.Sass_Value v);
	SassLibrary.Sass_Importer_List sass_make_importer_list(SizeT length);
	SassLibrary.Sass_Importer_Entry sass_importer_get_list_entry(SassLibrary.Sass_Importer_List list, SizeT idx);
	void sass_importer_set_list_entry(SassLibrary.Sass_Importer_List list, SizeT idx, SassLibrary.Sass_Importer_Entry entry);
	SassLibrary.Sass_Importer_Entry sass_make_importer(SassLibrary.Sass_Importer_Fn importer, double priority, Pointer cookie);
	SassLibrary.Sass_Importer_Fn sass_importer_get_function(SassLibrary.Sass_Importer_Entry cb);
	double sass_importer_get_priority(SassLibrary.Sass_Importer_Entry cb);
	Pointer sass_importer_get_cookie(SassLibrary.Sass_Importer_Entry cb);
	void sass_delete_importer(SassLibrary.Sass_Importer_Entry cb);
	SassLibrary.Sass_Import_List sass_make_import_list(SizeT length);
	SassLibrary.Sass_Import_Entry sass_make_import_entry(String path, ByteBuffer source, ByteBuffer srcmap);
	SassLibrary.Sass_Import_Entry sass_make_import(String imp_path, String abs_base, ByteBuffer source, ByteBuffer srcmap);
	SassLibrary.Sass_Import_Entry sass_import_set_error(SassLibrary.Sass_Import_Entry import$, String message, SizeT line, SizeT col);
	void sass_import_set_list_entry(SassLibrary.Sass_Import_List list, SizeT idx, SassLibrary.Sass_Import_Entry entry);
	SassLibrary.Sass_Import_Entry sass_import_get_list_entry(SassLibrary.Sass_Import_List list, SizeT idx);
	String sass_import_get_imp_path(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	String sass_import_get_abs_path(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	String sass_import_get_source(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	String sass_import_get_srcmap(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	Pointer sass_import_take_source(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	Pointer sass_import_take_srcmap(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	SizeT sass_import_get_error_line(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	SizeT sass_import_get_error_column(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	String sass_import_get_error_message(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	void sass_delete_import_list(SassLibrary.Sass_Import_List Sass_Import_List1);
	void sass_delete_import(SassLibrary.Sass_Import_Entry Sass_Import_Entry1);
	SassLibrary.Sass_Function_List sass_make_function_list(SizeT length);
	SassLibrary.Sass_Function_Entry sass_make_function(String signature, SassLibrary.Sass_Function_Fn cb, Pointer cookie);
	SassLibrary.Sass_Function_Entry sass_function_get_list_entry(SassLibrary.Sass_Function_List list, SizeT pos);
	void sass_function_set_list_entry(SassLibrary.Sass_Function_List list, SizeT pos, SassLibrary.Sass_Function_Entry cb);
	String sass_function_get_signature(SassLibrary.Sass_Function_Entry cb);
	SassLibrary.Sass_Function_Fn sass_function_get_function(SassLibrary.Sass_Function_Entry cb);
	SassLibrary.Sass_File_Context sass_make_file_context(String input_path);
	SassLibrary.Sass_Data_Context sass_make_data_context(ByteBuffer source_string);
	int sass_compile_file_context(SassLibrary.Sass_File_Context ctx);
	int sass_compile_data_context(SassLibrary.Sass_Data_Context ctx);
	SassLibrary.Sass_Compiler sass_make_file_compiler(SassLibrary.Sass_File_Context file_ctx);
	SassLibrary.Sass_Compiler sass_make_data_compiler(SassLibrary.Sass_Data_Context data_ctx);
	int sass_compiler_parse(SassLibrary.Sass_Compiler compiler);
	int sass_compiler_execute(SassLibrary.Sass_Compiler compiler);
	void sass_delete_compiler(SassLibrary.Sass_Compiler compiler);
	void sass_delete_file_context(SassLibrary.Sass_File_Context ctx);
	void sass_delete_data_context(SassLibrary.Sass_Data_Context ctx);
	SassLibrary.Sass_Context sass_file_context_get_context(SassLibrary.Sass_File_Context file_ctx);
	SassLibrary.Sass_Context sass_data_context_get_context(SassLibrary.Sass_Data_Context data_ctx);
	SassLibrary.Sass_Options sass_context_get_options(SassLibrary.Sass_Context ctx);
	SassLibrary.Sass_Options sass_file_context_get_options(SassLibrary.Sass_File_Context file_ctx);
	SassLibrary.Sass_Options sass_data_context_get_options(SassLibrary.Sass_Data_Context data_ctx);
	void sass_file_context_set_options(SassLibrary.Sass_File_Context file_ctx, SassLibrary.Sass_Options opt);
	void sass_data_context_set_options(SassLibrary.Sass_Data_Context data_ctx, SassLibrary.Sass_Options opt);
	int sass_option_get_precision(SassLibrary.Sass_Options options);
	int sass_option_get_output_style(SassLibrary.Sass_Options options);
	boolean sass_option_get_source_comments(SassLibrary.Sass_Options options);
	boolean sass_option_get_source_map_embed(SassLibrary.Sass_Options options);
	boolean sass_option_get_source_map_contents(SassLibrary.Sass_Options options);
	boolean sass_option_get_omit_source_map_url(SassLibrary.Sass_Options options);
	boolean sass_option_get_is_indented_syntax_src(SassLibrary.Sass_Options options);
	String sass_option_get_indent(SassLibrary.Sass_Options options);
	String sass_option_get_linefeed(SassLibrary.Sass_Options options);
	String sass_option_get_input_path(SassLibrary.Sass_Options options);
	String sass_option_get_output_path(SassLibrary.Sass_Options options);
	String sass_option_get_plugin_path(SassLibrary.Sass_Options options);
	SizeT sass_option_get_include_path_size(SassLibrary.Sass_Options options);
	String sass_option_get_include_path(SassLibrary.Sass_Options options, SizeT size);
	String sass_option_get_source_map_file(SassLibrary.Sass_Options options);
	String sass_option_get_source_map_root(SassLibrary.Sass_Options options);
	SassLibrary.Sass_Importer_List sass_option_get_c_headers(SassLibrary.Sass_Options options);
	SassLibrary.Sass_Importer_List sass_option_get_c_importers(SassLibrary.Sass_Options options);
	SassLibrary.Sass_Function_List sass_option_get_c_functions(SassLibrary.Sass_Options options);
	void sass_option_set_precision(SassLibrary.Sass_Options options, int precision);
	void sass_option_set_output_style(SassLibrary.Sass_Options options, int output_style);
	void sass_option_set_source_comments(SassLibrary.Sass_Options options, boolean source_comments);
	void sass_option_set_source_map_embed(SassLibrary.Sass_Options options, boolean source_map_embed);
	void sass_option_set_source_map_contents(SassLibrary.Sass_Options options, boolean source_map_contents);
	void sass_option_set_omit_source_map_url(SassLibrary.Sass_Options options, boolean omit_source_map_url);
	void sass_option_set_is_indented_syntax_src(SassLibrary.Sass_Options options, boolean is_indented_syntax_src);
	void sass_option_set_indent(SassLibrary.Sass_Options options, String indent);
	void sass_option_set_linefeed(SassLibrary.Sass_Options options, String linefeed);
	void sass_option_set_input_path(SassLibrary.Sass_Options options, String input_path);
	void sass_option_set_output_path(SassLibrary.Sass_Options options, String output_path);
	void sass_option_set_plugin_path(SassLibrary.Sass_Options options, String plugin_path);
	void sass_option_set_include_path(SassLibrary.Sass_Options options, String include_path);
	void sass_option_set_source_map_file(SassLibrary.Sass_Options options, String source_map_file);
	void sass_option_set_source_map_root(SassLibrary.Sass_Options options, String source_map_root);
	void sass_option_set_c_headers(SassLibrary.Sass_Options options, SassLibrary.Sass_Importer_List c_headers);
	void sass_option_set_c_importers(SassLibrary.Sass_Options options, SassLibrary.Sass_Importer_List c_importers);
	void sass_option_set_c_functions(SassLibrary.Sass_Options options, SassLibrary.Sass_Function_List c_functions);
	Pointer sass_context_get_output_string(SassLibrary.Sass_Context ctx);
	int sass_context_get_error_status(SassLibrary.Sass_Context ctx);
	String sass_context_get_error_json(SassLibrary.Sass_Context ctx);
	String sass_context_get_error_text(SassLibrary.Sass_Context ctx);
	String sass_context_get_error_message(SassLibrary.Sass_Context ctx);
	String sass_context_get_error_file(SassLibrary.Sass_Context ctx);
	String sass_context_get_error_src(SassLibrary.Sass_Context ctx);
	SizeT sass_context_get_error_line(SassLibrary.Sass_Context ctx);
	SizeT sass_context_get_error_column(SassLibrary.Sass_Context ctx);
	Pointer sass_context_get_source_map_string(SassLibrary.Sass_Context ctx);
	String[] sass_context_get_included_files(SassLibrary.Sass_Context ctx);
	SizeT sass_context_get_included_files_size(SassLibrary.Sass_Context ctx);
	PointerByReference sass_context_take_included_files(SassLibrary.Sass_Context ctx);
	int sass_compiler_get_state(SassLibrary.Sass_Compiler compiler);
	SassLibrary.Sass_Context sass_compiler_get_context(SassLibrary.Sass_Compiler compiler);
	SassLibrary.Sass_Options sass_compiler_get_options(SassLibrary.Sass_Compiler compiler);
	SizeT sass_compiler_get_import_stack_size(SassLibrary.Sass_Compiler compiler);
	SassLibrary.Sass_Import_Entry sass_compiler_get_last_import(SassLibrary.Sass_Compiler compiler);
	void sass_option_push_plugin_path(SassLibrary.Sass_Options options, String path);
	void sass_option_push_include_path(SassLibrary.Sass_Options options, String path);
	class Sass_Function_Entry extends PointerType {
		public Sass_Function_Entry(Pointer address) {
			super(address);
		}
		public Sass_Function_Entry() {
			super();
		}
	}

	class Sass_Context extends PointerType {
		public Sass_Context(Pointer address) {
			super(address);
		}
		public Sass_Context() {
			super();
		}
	}

	class Sass_Importer_Entry extends PointerType {
		public Sass_Importer_Entry(Pointer address) {
			super(address);
		}
		public Sass_Importer_Entry() {
			super();
		}
	}

	class Sass_Value extends PointerType {
		public Sass_Value(Pointer address) {
			super(address);
		}
		public Sass_Value() {
			super();
		}
	}

	class Sass_Function_List extends PointerType {
		public Sass_Function_List(Pointer address) {
			super(address);
		}
		public Sass_Function_List() {
			super();
		}
	}

	class Sass_Options extends PointerType {
		public Sass_Options(Pointer address) {
			super(address);
		}
		public Sass_Options() {
			super();
		}
	}

	class Sass_Compiler extends PointerType {
		public Sass_Compiler(Pointer address) {
			super(address);
		}
		public Sass_Compiler() {
			super();
		}
	}

	class Sass_File_Context extends PointerType {
		public Sass_File_Context(Pointer address) {
			super(address);
		}
		public Sass_File_Context() {
			super();
		}
	}

	class Sass_Import_List extends PointerType {
		public Sass_Import_List(Pointer address) {
			super(address);
		}
		public Sass_Import_List() {
			super();
		}
	}

	class Sass_Importer_List extends PointerType {
		public Sass_Importer_List(Pointer address) {
			super(address);
		}
		public Sass_Importer_List() {
			super();
		}
	}

	class Sass_Import_Entry extends PointerType {
		public Sass_Import_Entry(Pointer address) {
			super(address);
		}
		public Sass_Import_Entry() {
			super();
		}
	}

	class Sass_Data_Context extends PointerType {
		public Sass_Data_Context(Pointer address) {
			super(address);
		}
		public Sass_Data_Context() {
			super();
		}
	}
}
