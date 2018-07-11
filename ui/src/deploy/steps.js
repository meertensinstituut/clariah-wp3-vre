import React from "react";
import {Pagination} from 'react-bootstrap';
import {withRouter} from 'react-router-dom';
import queryString from 'query-string';
import PropTypes from 'prop-types';

class Steps extends React.Component {

    constructor(props) {
        super(props);
        let params = queryString.parse(this.props.location.search);
        params = normalizeParams(params);
        updateUrlParams(params, this.props.history);
        this.state = {params};
    }

    static getDerivedStateFromProps(nextProps, prevState) {
        let params = convertStepsToParams(nextProps.steps);
        params = normalizeParams(params);
        let nextParams = JSON.stringify(params);
        let prevParams = JSON.stringify(prevState.params);
        if (prevState.params !== undefined && nextParams !== prevParams) {
            updateUrlParams(params, nextProps.history);
            return {params: params};
        }
        return null;
    }

    isDisabled(active, completed, i) {
        let disabled = true;
        if (i <= active) {
            disabled = false;
        } else if (i === active + 1) {
            disabled = !completed;
        }
        return disabled;
    }

    render() {
        let active = this.props.steps.findIndex((s) => s.key === this.props.active);
        return (
            <div>
                <Pagination>
                    {this.props.steps.map((step, i) => {
                        let disabled = this.isDisabled(active, this.props.completed, i);
                        return (
                            <Pagination.Item
                                key={i}
                                onClick={() => step.callback()}
                                active={i === active}
                                disabled={disabled}
                            >
                                {step.label}
                            </Pagination.Item>
                        );
                    }, this)}
                </Pagination>
            </div>
        );
    }
}


function normalizeParams(params) {
    const result = {};
    const temp = Object.assign({}, params);
    Object.keys(params).forEach((key) => {
        temp[key] = stringToBoolean(params[key]);
        if(temp[key] === false) delete temp[key];
    });
    Object
        .keys(temp)
        .sort()
        .forEach(key => result[key] = temp[key]);
    return result;
}

function stringToBoolean(val) {
    return val === 'true' || (val === 'false' ? false : val);
}

function convertStepsToParams(steps) {
    let params = {};
    steps.forEach((s) => params[s.key] = s.value);
    return params;
}

function updateUrlParams(params, history) {
    history.push('/deploy?' + queryString.stringify(params));
}


Steps.propTypes = {
    steps: PropTypes.array.isRequired,
    active: PropTypes.string.isRequired,
    completed: PropTypes.bool.isRequired
};

export default withRouter(Steps);