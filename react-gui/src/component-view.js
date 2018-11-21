import React from 'react';
import mdStatic from 'markdown-it';

const md = mdStatic();

/**
 * 
 * @param {string} id
 * @param {string} cssCode 
 */
function addStyleTag(id, cssCode) {
    if (!document.getElementById(id)) {
        const style = document.createElement('style');
        style.type = 'text/css';
        style.appendChild(document.createTextNode(cssCode));
        style.id = id;
        document.head.appendChild(style);
    }
}

/**
 * 
 * @param {TemplateStringsArray} strings 
 * @param {any[]} keys 
 */
function css(strings, ...keys) {
    const lastIndex = strings.length - 1;
    return strings.slice(0, lastIndex).reduce((acc, cur, index) => acc + cur + keys[index], '') + strings[lastIndex];
}

function addStyles() {
    addStyleTag(
        'storybook_style_injected',
        css`
            @import url('https://fonts.googleapis.com/css?family=Roboto:300,300i,700');
            @import url('https://fonts.googleapis.com/css?family=Roboto+Mono:300,300i');

            .hds {
                display: flex;
            }

            .hds-aside {
                font-family: 'Roboto', sans-serif;
                font-weight: 300;
                font-size: 16px;
                line-height: 140%;
                font-style: normal;
                color: #3e3e3e;

                background-color: #fafafa;
                width: 200px;
                padding: 1.5em;
                display: flex;
                flex-direction: column;
                min-height: 100vh;
            }

            .hds-aside-logo {
                position: fixed;
                width: 70px;
                height: auto;
                margin: 0 0 2em 0;
            }

            .hds-nav {
                display: flex;
                position: fixed;
                top: 110px;
                flex-direction: column;
            }

            .hds-content {
                width: calc(100% - 200px);
                padding: 3em;
            }

            .hds-nav-wrapper {
                display: flex;
                flex-direction: column;
            }

            .hds-nav-item {
                margin: 0 0 1em 0;
                color: #0087d4;
                text-decoration: none;
            }

            .hds-showComp {
                margin: 0 0 6em 0;
            }

            .hds-showComp-title {
                font-family: 'Roboto', sans-serif;
                font-weight: bold;
            }

            .hds-showComp-components {
                background-color: #f5efe9;
                padding: 1em;
                margin: 0 -1em;
            }

            .hds-showComp-code {
                padding: 1em;
                margin: 0 -1em;
                background-color: #fafafa;
            }

            .hds-showComp-code pre {
                margin: 0 !important;
            }

            .hds-showComp-label {
                font-size: 0.7em;
                float: right;
                opacity: 0.3;
                margin-top: -1em;
            }

            .markdown-body {
                font-family: 'Roboto', sans-serif;
                font-weight: 300;
                font-size: 16px;
                line-height: 140%;
                font-style: normal;
                color: #3e3e3e;
            }

            .hds-showComp-code-text {
                font-family: 'Roboto Mono', monospace;
                font-weight: 300;
                font-size: 14px;
                line-height: 140%;
                white-space: pre;
                overflow-x: scroll;
                display: block;
            }

            .hds-content-section-title {
                font-family: 'Roboto', sans-serif;
                font-size: 2em;
                line-height: 130%;
                font-weight: 700;
            }

            @media (max-width: 700px) {
                .hds-aside {
                    display: none;
                }
                .hds-content {
                    width: 100%;
                }
            }

            @media (min-width: 600px) {
                .hds-content-section-title {
                    font-size: 2em;
                }
                .markdown-body,
                .hds-aside {
                    font-size: 18px;
                    line-height: 150%;
                }
            }
        `
    );
}

/**
 * 
 * @param {any} val 
 */
function asArray(val) {
    return Array.isArray(val) ? val : val == null ? [] : [val];
}

/**
 * 
 * @param {any} defaultProps 
 * @param {any} key 
 * @param {any} value 
 */
function isDefaultProp(defaultProps, key, value) {
    if (!defaultProps) {
        return false;
    }
    return defaultProps[key] === value;
}

/**
 * 
 * @param {any} object 
 * @param {{ useFunctionCode?: boolean; functionNameOnly?: boolean }} opts 
 * 
 * @returns {string}
 */
function stringifyObject(object, opts= {}) {
    if (object == null) {
        // remove null and undefined first
        return object + '';
    } else if (typeof object === 'string') {
        return JSON.stringify(object); // escape string
    } else if (Array.isArray(object)) {
        return JSON.stringify(object.map(it => stringifyObject(it, opts)));
    } else if (typeof object === 'function') {
        if (opts.useFunctionCode) {
            if (opts.functionNameOnly) {
                return object.name.toString();
            } else {
                return object.toString();
            }
        } else {
            return 'function () {...}';
        }
    } else if (typeof object === 'object') {
        if (React.isValidElement(object)) {
            return jsxToString(object, opts);
        }
        return '{ ' +
          Object.keys(object).map(key => {
              return JSON.stringify(key) + ': ' + stringifyObject(object[key]);
          }).join(", ") + '}'
    } else {
        // all other primitives
        return object + '';
    }
}

const _JSX_REGEXP = /"<.+>"/g;

/**
 * 
 * @param {any} item 
 * @param {any} options 
 * @param {boolean} delimit 
 */
function serializeItem(item, options, delimit = true) {
    /** @type {string} */
    let result;

    if (typeof item === 'string') {
        result = delimit ? `'${item}'` : item;
    } else if (typeof item === 'number' || typeof item === 'boolean') {
        result = `${item}`;
    } else if (Array.isArray(item)) {
        const indentation = new Array(options.spacing + 1).join(' ');
        const delimiter = delimit ? ', ' : `\n${indentation}`;
        const items = item.map(i => serializeItem(i, options)).join(delimiter);
        result = delimit ? `[${items}]` : `${items}`;
    } else if (React.isValidElement(item)) {
        result = jsxToString(item, options);
    } else if (typeof item === 'object') {
        result = stringifyObject(item, options);
        // remove string quotes from embeded JSX values
        result = result.replace(_JSX_REGEXP, function(match) {
            return match.slice(1, match.length - 1);
        });
    } else if (typeof item === 'function') {
        result = options.useFunctionCode
            ? options.functionNameOnly ? item.name.toString() : item.toString()
            : `() => ...`;
    } else {
        result = '(UNKNOWN)';
    }
    return result;
}

/**
 * 
 * @param {any} component 
 * @param {any} options 
 */
function jsxToString(component, options) {
    const baseOpts = {
        displayName: component.type.displayName || component.type.name || component.type,
        ignoreProps: [],
        ignoreTags: [],
        keyValueOverride: {},
        spacing: 0,
        detectFunctions: false
    };

    const opts = { ...baseOpts, ...options };

    // Do not return anything if the root tag should be ignored
    if (opts.ignoreTags.indexOf(opts.displayName) !== -1) {
        return '';
    }

    /** @type {{ name: any; props?: any; children?: any }} */
    const componentData = {
        name: opts.displayName
    };

    delete opts.displayName;
    if (component.props) {
        const indentation = new Array(opts.spacing + 3).join(' ');
        componentData.props = Object.keys(component.props)
            .filter(key => {
                return (
                    key !== 'children' &&
                    !isDefaultProp(component.type.defaultProps, key, component.props[key]) &&
                    opts.ignoreProps.indexOf(key) === -1
                );
            })
            .map(key => {
                let value;
                if (typeof opts.keyValueOverride[key] === 'function') {
                    value = opts.keyValueOverride[key](component.props[key]);
                } else if (opts.keyValueOverride[key]) {
                    value = opts.keyValueOverride[key];
                } else if (
                    opts.shortBooleanSyntax &&
                    typeof component.props[key] === 'boolean' &&
                    component.props[key]
                ) {
                    return key;
                } else {
                    value = serializeItem(component.props[key], { ...opts, key });
                }
                if (typeof value !== 'string' || value[0] !== "'") {
                    value = `{${value}}`;
                }
                // Is `value` a multi-line string?
                const valueLines = value.split(/\r\n|\r|\n/);
                if (valueLines.length > 1) {
                    value = valueLines.join(`\n${indentation}`);
                }
                return `${key}=${value}`;
            })
            .join(`\n${indentation}`);

        if (component.key && opts.ignoreProps.indexOf('key') === -1) {
            componentData.props += `key='${component.key}'`;
        }

        if (componentData.props.length > 0) {
            componentData.props = ' ' + componentData.props;
        }
    }

    if (component.props.children) {
        opts.spacing += 2;
        const indentation = new Array(opts.spacing + 1).join(' ');
        if (Array.isArray(component.props.children)) {
            componentData.children = component.props.children
                .reduce((coll, item) => coll.concat(item), []) // handle Array of Arrays
                .filter((child) => {
                    const childShouldBeRemoved =
                        child &&
                        child.type &&
                        opts.ignoreTags.indexOf(child.type.displayName || child.type.name || child.type) === -1;
                    // Filter the tag if it is in the ignoreTags list or if is not a tag
                    return childShouldBeRemoved;
                })
                .map((child) => serializeItem(child, opts, false))
                .join(`\n${indentation}`);
        } else {
            componentData.children = serializeItem(component.props.children, opts, false);
        }
        return (
            `<${componentData.name}${componentData.props}>\n` +
            `${indentation}${componentData.children}\n` +
            `${indentation.slice(0, -2)}</${componentData.name}>`
        );
    } else {
        return `<${componentData.name}${componentData.props} />`;
    }
}

export class DescribedMock extends React.Component {
    render() {
        const children = asArray(this.props.children).map(function(child) {
            if (typeof child === 'string') {
                const lines = child.split('\n');
                if (lines[0] === '') {
                    lines.shift();
                }
                if (lines.length === 0) {
                    return null;
                }
                const indentation = (lines[0].match(/^ */) || [''])[0].length;
                const unIndented = lines
                    .map(x => x.replace(new RegExp('^ {0,' + indentation + '}'), ''))
                    .map(x => x.replace(new RegExp('^#'), '##'))
                    .join('\n');
                return <div className="markdown-body" dangerouslySetInnerHTML={{ __html: md.render(unIndented) }} />;
            } else {
                return child;
            }
        });
        return (
            <section id={this.props.title} className="hds-content-section">
                <h1 className="hds-content-section-title">{this.props.title}</h1>
                {children}
            </section>
        );
    }
}

export class StyleGuide extends React.Component {
    componentDidMount() {
        addStyles();
    }
    render() {
        document.body.style.overflow = null;
        return (
            <div className="hds">
                <div className="hds-aside">
                    <img src="/images/logo-knaw-humanities-cluster.png" alt="" className="hds-aside-logo" />
                    <div className="hds-nav">
                        <nav className="hds-nav-wrapper">
                            {asArray(this.props.children).map(it => {
                                return (
                                    <a className="hds-nav-item" key={it.props.title} href={'#' + it.props.title}>
                                        {it.props.title}
                                    </a>
                                );
                            })}
                        </nav>
                    </div>
                </div>
                <div className="hds-content">
                    {asArray(this.props.children).map(it => <div key={it.props.title}>{it}</div>)}
                </div>
            </div>
        );
    }
}

export function withDefaults(name, elm, defaults) {
    const result = (props) => elm(Object.assign({}, defaults, props));
    result.displayName = name; // required for the source code preview in the optimized builds to work
    return result;
}

export const ContentBlock = ({
    color = 'violet',
    width = 100,
    height = 100
}) => <div style={{ backgroundColor: color, width: width, height: height }} />;
ContentBlock.displayName = 'ContentBlock';

export class Embed extends React.Component {
    render() {
        const props = this.props;
        let wrapperStyle = {};
        let outerElmStyle = {};
        if (props.fullscreen) {
            if (props.fullscreen === 'FILL') {
                document.body.style.overflow = 'hidden';
                document.body.style.height = '100vh';
                outerElmStyle = {
                    position: 'absolute',
                    top: 0,
                    left: 0,
                    width: '100vw',
                    height: '100vh',
                    backgroundColor: 'white',
                    overflow: 'scroll'
                };
                return (
                    <div className="compOuter" style={outerElmStyle}>
                        {props.children}
                    </div>
                );
            } else {
                outerElmStyle = {
                    width: '100vw',
                    height: '100vh',
                    transform: props.fullscreen === 'BIG' ? 'scale(0.8)' : 'scale(0.4)',
                    transformOrigin: 'top left',
                    boxShadow: 'darkgrey 0px 0px 20px',
                    backgroundColor: 'white',
                    overflow: 'scroll'
                };
                wrapperStyle = {
                    boxSizing: 'content-box',
                    height: props.fullscreen === 'BIG' ? '80vh' : '40vh'
                };
            }
        }
        return (
            <div className="hds-showComp">
                <div className="hds-showComp-title">{props.caption}</div>
                <div className="hds-showComp-description">{props.description}</div>
                <div className="compWrapper hds-showComp-components" style={wrapperStyle}>
                    <div className="hds-showComp-label">Component</div>
                    <div className="compOuter" style={outerElmStyle}>
                        {props.children}
                    </div>
                </div>
                <div className="hds-showComp-code">
                    <div className="hds-showComp-label">Code usage</div>
                    <code className="hds-showComp-code-text">{jsxToString(props.children)}</code>
                </div>
            </div>
        );
    }
}
